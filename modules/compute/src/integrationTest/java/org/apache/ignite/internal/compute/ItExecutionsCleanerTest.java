/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.compute;

import static org.apache.ignite.internal.TestWrappers.unwrapIgniteImpl;
import static org.apache.ignite.internal.testframework.matchers.CompletableFutureExceptionMatcher.willThrow;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Set;
import java.util.UUID;
import org.apache.ignite.Ignite;
import org.apache.ignite.compute.ComputeException;
import org.apache.ignite.compute.IgniteCompute;
import org.apache.ignite.compute.JobDescriptor;
import org.apache.ignite.compute.JobTarget;
import org.apache.ignite.internal.ClusterPerClassIntegrationTest;
import org.apache.ignite.internal.compute.utils.InteractiveJobs;
import org.apache.ignite.internal.compute.utils.TestingJobExecution;
import org.apache.ignite.internal.wrapper.Wrappers;
import org.apache.ignite.lang.CancelHandle;
import org.apache.ignite.lang.CancellationToken;
import org.apache.ignite.network.ClusterNode;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItExecutionsCleanerTest extends ClusterPerClassIntegrationTest {
    private ExecutionManager localExecutionManager;

    private ExecutionManager remoteExecutionManager;

    private Set<ClusterNode> localNodes;

    private Set<ClusterNode> remoteNodes;

    @Override
    protected int[] cmgMetastoreNodes() {
        return new int[]{0, 1, 2};
    }

    @Override
    protected String getNodeBootstrapConfigTemplate() {
        return "ignite {\n"
                + "  network: {\n"
                + "    port: {},\n"
                + "    nodeFinder.netClusterNodes: [ {} ]\n"
                + "  },\n"
                + "  clientConnector.port: {},\n"
                + "  rest.port: {},\n"
                + "  compute: {"
                + "    threadPoolSize: 1,\n"
                + "    statesLifetimeMillis: 1000\n"
                + "  },\n"
                + "  failureHandler.dumpThreadsOnFailure: false\n"
                + "}";
    }

    @BeforeEach
    void setUp() {
        // Get new references before each test since node can be restarted.
        Ignite localNode = CLUSTER.node(0);
        Ignite remoteNode = CLUSTER.node(1);

        IgniteComputeImpl localCompute = unwrapIgniteComputeImpl(localNode.compute());
        IgniteComputeImpl remoteCompute = unwrapIgniteComputeImpl(remoteNode.compute());

        localExecutionManager = ((ComputeComponentImpl) localCompute.computeComponent()).executionManager();
        remoteExecutionManager = ((ComputeComponentImpl) remoteCompute.computeComponent()).executionManager();

        localNodes = Set.of(unwrapIgniteImpl(localNode).node());
        remoteNodes = Set.of(unwrapIgniteImpl(remoteNode).node());
    }

    private static IgniteComputeImpl unwrapIgniteComputeImpl(IgniteCompute compute) {
        return Wrappers.unwrap(compute, IgniteComputeImpl.class);
    }

    @Test
    void localCompleted() throws Exception {
        TestingJobExecution<Object> execution = submit(localNodes);
        UUID jobId = execution.idSync();

        // Complete the task
        InteractiveJobs.globalJob().finish();
        execution.assertCompleted();

        // Execution is retained
        assertThat(localExecutionManager.localExecutions(), hasItem(jobId));

        // And eventually cleaned
        await().untilAsserted(() -> {
            assertThat(localExecutionManager.localExecutions(), is(empty()));

            assertThat(localExecutionManager.resultAsync(jobId), willThrow(ComputeException.class));
        });
    }

    @Test
    void localCancelled() throws Exception {
        // Start first task
        CancelHandle runningCancelHandle = CancelHandle.create();
        TestingJobExecution<Object> runningExecution = submit(localNodes, runningCancelHandle.token());
        UUID runningJobId = runningExecution.idSync();

        // Start second task
        CancelHandle queuedCancelHandle = CancelHandle.create();
        TestingJobExecution<Object> queuedExecution = submit(localNodes, queuedCancelHandle.token());
        UUID queuedJobId = queuedExecution.idSync();

        // Second task is queued, cancel it
        queuedExecution.assertQueued();
        queuedCancelHandle.cancel();
        queuedExecution.assertCancelled();

        // First task is executing, cancel it
        runningExecution.assertExecuting();
        runningCancelHandle.cancel();
        runningExecution.assertCancelled();

        // All executions are retained
        assertThat(localExecutionManager.localExecutions(), hasItem(runningJobId));
        assertThat(localExecutionManager.localExecutions(), hasItem(queuedJobId));

        // And eventually cleaned
        await().untilAsserted(() -> {
            assertThat(localExecutionManager.localExecutions(), is(empty()));

            assertThat(localExecutionManager.resultAsync(runningJobId), willThrow(ComputeException.class));
            assertThat(localExecutionManager.resultAsync(queuedJobId), willThrow(ComputeException.class));
        });
    }

    @Test
    void remoteCompleted() throws Exception {
        // Completed
        TestingJobExecution<Object> execution = submit(remoteNodes);
        UUID jobId = execution.idSync();

        // Complete the task
        InteractiveJobs.globalJob().finish();
        execution.assertCompleted();

        // Execution is retained
        assertThat(localExecutionManager.remoteExecutions(), hasItem(jobId));
        assertThat(remoteExecutionManager.localExecutions(), hasItem(jobId));

        // And eventually cleaned
        await().untilAsserted(() -> {
            assertThat(localExecutionManager.remoteExecutions(), is(empty()));
            assertThat(remoteExecutionManager.localExecutions(), is(empty()));

            assertThat(localExecutionManager.resultAsync(jobId), willThrow(ComputeException.class));
            assertThat(remoteExecutionManager.resultAsync(jobId), willThrow(ComputeException.class));
        });
    }

    @Test
    void remoteCancelled() throws Exception {
        // Start first task
        CancelHandle runningCancelHandle = CancelHandle.create();
        TestingJobExecution<Object> runningExecution = submit(remoteNodes, runningCancelHandle.token());
        UUID runningJobId = runningExecution.idSync();

        // Start second task
        CancelHandle queuedCancelHandle = CancelHandle.create();
        TestingJobExecution<Object> queuedExecution = submit(remoteNodes, queuedCancelHandle.token());
        UUID queuedJobId = queuedExecution.idSync();

        // Second task is queued, cancel it
        queuedExecution.assertQueued();
        queuedCancelHandle.cancel();
        queuedExecution.assertCancelled();

        // First task is executing, cancel it
        runningExecution.assertExecuting();
        runningCancelHandle.cancel();
        runningExecution.assertCancelled();

        // All executions are retained
        assertThat(localExecutionManager.remoteExecutions(), hasItem(runningJobId));
        assertThat(remoteExecutionManager.localExecutions(), hasItem(queuedJobId));

        // And eventually cleaned
        await().untilAsserted(() -> {
            assertThat(localExecutionManager.remoteExecutions(), is(empty()));
            assertThat(remoteExecutionManager.localExecutions(), is(empty()));

            assertThat(localExecutionManager.resultAsync(runningJobId), willThrow(ComputeException.class));
            assertThat(remoteExecutionManager.resultAsync(runningJobId), willThrow(ComputeException.class));
            assertThat(localExecutionManager.resultAsync(queuedJobId), willThrow(ComputeException.class));
            assertThat(remoteExecutionManager.resultAsync(queuedJobId), willThrow(ComputeException.class));
        });
    }

    @Test
    void failover() throws Exception {
        TestingJobExecution<Object> execution = submit(
                Set.of(unwrapIgniteImpl(CLUSTER.node(1)).node(), unwrapIgniteImpl(CLUSTER.node(2)).node())
        );
        UUID jobId = execution.idSync();

        execution.assertExecuting();

        // Stop the worker node
        String workerNodeName = InteractiveJobs.globalJob().currentWorkerName();
        int workerNodeIndex = CLUSTER.nodeIndex(workerNodeName);
        CLUSTER.stopNode(workerNodeIndex);

        String failoverWorkerNodeName = InteractiveJobs.globalJob().currentWorkerName();

        Ignite failoverNode = CLUSTER.node(CLUSTER.nodeIndex(failoverWorkerNodeName));
        IgniteComputeImpl failoverCompute = unwrapIgniteComputeImpl(failoverNode.compute());
        ExecutionManager failoverExecutionManager = ((ComputeComponentImpl) failoverCompute.computeComponent()).executionManager();

        InteractiveJobs.globalJob().assertAlive();
        execution.assertExecuting();

        // Complete the task
        InteractiveJobs.globalJob().finish();
        execution.assertCompleted();

        // Execution is retained
        assertThat(localExecutionManager.remoteExecutions(), hasItem(jobId));
        // Job id on the failover node is different, so just check that something is stored there
        assertThat(failoverExecutionManager.localExecutions(), is(not(empty())));

        // And eventually cleaned
        await().untilAsserted(() -> {
            assertThat(localExecutionManager.remoteExecutions(), is(empty()));
            assertThat(failoverExecutionManager.localExecutions(), is(empty()));
        });

        // Start node again for next tests
        CLUSTER.startNode(workerNodeIndex);
    }

    private static TestingJobExecution<Object> submit(Set<ClusterNode> nodes) {
        return submit(nodes, null);
    }

    private static TestingJobExecution<Object> submit(Set<ClusterNode> nodes, @Nullable CancellationToken cancellationToken) {
        return new TestingJobExecution<>(CLUSTER.node(0).compute().submitAsync(
                JobTarget.anyNode(nodes), JobDescriptor.builder(InteractiveJobs.globalJob().name()).build(), null, cancellationToken
        ));
    }
}
