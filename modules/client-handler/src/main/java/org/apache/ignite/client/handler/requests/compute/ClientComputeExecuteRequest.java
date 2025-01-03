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

package org.apache.ignite.client.handler.requests.compute;

import static org.apache.ignite.client.handler.requests.compute.ClientComputeGetStateRequest.packJobState;
import static org.apache.ignite.internal.client.proto.ClientComputeJobUnpacker.unpackJobArgumentWithoutMarshaller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.client.handler.NotificationSender;
import org.apache.ignite.compute.JobExecution;
import org.apache.ignite.compute.JobExecutionOptions;
import org.apache.ignite.compute.NodeNotFoundException;
import org.apache.ignite.deployment.DeploymentUnit;
import org.apache.ignite.internal.client.proto.ClientComputeJobPacker;
import org.apache.ignite.internal.client.proto.ClientMessagePacker;
import org.apache.ignite.internal.client.proto.ClientMessageUnpacker;
import org.apache.ignite.internal.compute.IgniteComputeInternal;
import org.apache.ignite.internal.compute.MarshallerProvider;
import org.apache.ignite.internal.network.ClusterService;
import org.apache.ignite.marshalling.Marshaller;
import org.apache.ignite.network.ClusterNode;
import org.jetbrains.annotations.Nullable;

/**
 * Compute execute request.
 */
public class ClientComputeExecuteRequest {
    /**
     * Processes the request.
     *
     * @param in Unpacker.
     * @param out Packer.
     * @param compute Compute.
     * @param cluster Cluster.
     * @param notificationSender Notification sender.
     * @return Future.
     */
    public static CompletableFuture<Void> process(
            ClientMessageUnpacker in,
            ClientMessagePacker out,
            IgniteComputeInternal compute,
            ClusterService cluster,
            NotificationSender notificationSender
    ) {
        Set<ClusterNode> candidates = unpackCandidateNodes(in, cluster);

        List<DeploymentUnit> deploymentUnits = in.unpackDeploymentUnits();
        String jobClassName = in.unpackString();
        JobExecutionOptions options = JobExecutionOptions.builder().priority(in.unpackInt()).maxRetries(in.unpackInt()).build();
        Object arg = unpackJobArgumentWithoutMarshaller(in);

        JobExecution<Object> execution = compute.executeAsyncWithFailover(
                candidates, deploymentUnits, jobClassName, options, null, arg
        );
        sendResultAndState(execution, notificationSender);

        //noinspection DataFlowIssue
        return execution.idAsync().thenAccept(out::packUuid);
    }

    private static Set<ClusterNode> unpackCandidateNodes(ClientMessageUnpacker in, ClusterService cluster) {
        int size = in.unpackInt();

        if (size < 1) {
            throw new IllegalArgumentException("nodes must not be empty.");
        }

        Set<String> nodeNames = new HashSet<>(size);
        Set<ClusterNode> nodes = new HashSet<>(size);

        for (int i = 0; i < size; i++) {
            String nodeName = in.unpackString();
            nodeNames.add(nodeName);
            ClusterNode node = cluster.topologyService().getByConsistentId(nodeName);
            if (node != null) {
                nodes.add(node);
            }
        }

        if (nodes.isEmpty()) {
            throw new NodeNotFoundException(nodeNames);
        }

        return nodes;
    }

    static CompletableFuture<Object> sendResultAndState(
            JobExecution<Object> execution,
            NotificationSender notificationSender
    ) {
        return execution.resultAsync().whenComplete((val, err) ->
                execution.stateAsync().whenComplete((state, errState) ->
                        notificationSender.sendNotification(w -> {
                            Marshaller<Object, byte[]> marshaller = extractMarshaller(execution);
                            ClientComputeJobPacker.packJobResult(val, marshaller, w);
                            packJobState(w, state);
                        }, err)));
    }

    private static <T> @Nullable Marshaller<T, byte[]> extractMarshaller(JobExecution<T> e) {
        if (e instanceof MarshallerProvider) {
            return ((MarshallerProvider<T>) e).resultMarshaller();
        }

        return null;
    }
}
