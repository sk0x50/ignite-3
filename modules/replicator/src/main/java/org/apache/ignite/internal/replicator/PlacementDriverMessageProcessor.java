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

package org.apache.ignite.internal.replicator;

import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;
import static org.apache.ignite.internal.replicator.ReplicatorRecoverableExceptions.isRecoverable;
import static org.apache.ignite.internal.util.ExceptionUtils.hasCause;
import static org.apache.ignite.internal.util.IgniteUtils.retryOperationUntilSuccess;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import org.apache.ignite.internal.hlc.ClockService;
import org.apache.ignite.internal.hlc.HybridTimestamp;
import org.apache.ignite.internal.lang.ComponentStoppingException;
import org.apache.ignite.internal.lang.NodeStoppingException;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.internal.network.NetworkMessage;
import org.apache.ignite.internal.placementdriver.PlacementDriver;
import org.apache.ignite.internal.placementdriver.message.LeaseGrantedMessage;
import org.apache.ignite.internal.placementdriver.message.LeaseGrantedMessageResponse;
import org.apache.ignite.internal.placementdriver.message.PlacementDriverMessagesFactory;
import org.apache.ignite.internal.placementdriver.message.PlacementDriverReplicaMessage;
import org.apache.ignite.internal.raft.Peer;
import org.apache.ignite.internal.raft.client.TopologyAwareRaftGroupService;
import org.apache.ignite.internal.replicator.message.PrimaryReplicaChangeCommand;
import org.apache.ignite.internal.replicator.message.ReplicaMessagesFactory;
import org.apache.ignite.internal.util.PendingComparableValuesTracker;
import org.apache.ignite.internal.util.TrackerClosedException;
import org.apache.ignite.network.ClusterNode;
import org.jetbrains.annotations.Nullable;

/**
 * Replica specific placement driver message processor.
 */
public class PlacementDriverMessageProcessor {
    private static final IgniteLogger LOG = Loggers.forClass(PlacementDriverMessageProcessor.class);

    private static final PlacementDriverMessagesFactory PLACEMENT_DRIVER_MESSAGES_FACTORY = new PlacementDriverMessagesFactory();

    private static final ReplicaMessagesFactory REPLICA_MESSAGES_FACTORY = new ReplicaMessagesFactory();

    private final ReplicationGroupId groupId;

    private final ClusterNode localNode;

    private final PlacementDriver placementDriver;

    private final ClockService clockService;

    private final BiConsumer<ReplicationGroupId, HybridTimestamp> replicaReservationClosure;

    // TODO: IGNITE-20063 Maybe get rid of it
    private final Executor executor;

    /** Latest lease expiration time. */
    private volatile HybridTimestamp leaseExpirationTime;

    private final PendingComparableValuesTracker<Long, Void> storageIndexTracker;

    // TODO IGNITE-19120 after replica inoperability logic is introduced, this future should be replaced with something like
    //     VersionedValue (so that PlacementDriverMessages would wait for new leader election)
    /** Completes when leader is elected. */
    private final CompletableFuture<Void> leaderReadyFuture = new CompletableFuture<>();

    /** Container of the elected leader. */
    private volatile @Nullable ClusterNode leaderRef = null;

    private final TopologyAwareRaftGroupService raftClient;

    /**
     * The constructor of a replica server.
     *
     * @param groupId Replication group id.
     * @param localNode Instance of the local node.
     * @param placementDriver Placement driver.
     * @param clockService Clock service.
     * @param replicaReservationClosure Closure that will be called to reserve the replica for becoming primary. It returns whether
     *     the reservation was successful.
     * @param executor Executor for handling requests.
     * @param storageIndexTracker Storage index tracker.
     * @param raftClient Raft client.
     */
    PlacementDriverMessageProcessor(
            ReplicationGroupId groupId,
            ClusterNode localNode,
            PlacementDriver placementDriver,
            ClockService clockService,
            BiConsumer<ReplicationGroupId, HybridTimestamp> replicaReservationClosure,
            Executor executor,
            PendingComparableValuesTracker<Long, Void> storageIndexTracker,
            TopologyAwareRaftGroupService raftClient
    ) {
        this.groupId = groupId;
        this.localNode = localNode;
        this.placementDriver = placementDriver;
        this.clockService = clockService;
        this.replicaReservationClosure = replicaReservationClosure;
        this.executor = executor;
        this.storageIndexTracker = storageIndexTracker;
        this.raftClient = raftClient;

        raftClient.subscribeLeader(this::onLeaderElected);
    }

    /**
     * Process placement driver message.
     *
     * @param msg Message to process.
     * @return Future that contains a result.
     */
    CompletableFuture<? extends NetworkMessage> processPlacementDriverMessage(PlacementDriverReplicaMessage msg) {
        if (msg instanceof LeaseGrantedMessage) {
            return processLeaseGrantedMessage((LeaseGrantedMessage) msg)
                    .handle((v, e) -> {
                        if (e != null) {
                            if (!hasCause(e, NodeStoppingException.class, ComponentStoppingException.class, TrackerClosedException.class)
                                    && !isRecoverable(e)) {
                                LOG.warn("Failed to process the lease granted message, lease negotiation will be retried [msg={}].",
                                        e, msg);
                            }

                            // Just restart the negotiation in case of exception.
                            return PLACEMENT_DRIVER_MESSAGES_FACTORY.leaseGrantedMessageResponse()
                                    .accepted(false)
                                    .build();
                        } else {
                            return v;
                        }
                    });
        }

        return failedFuture(new AssertionError("Unknown message type, msg=" + msg));
    }

    /**
     * Process lease granted message. Can either accept lease or decline with redirection proposal. In the case of lease acceptance,
     * initiates the leadership transfer, if this replica is not a group leader.
     *
     * @param msg Message to process.
     * @return Future that contains a result.
     */
    private CompletableFuture<LeaseGrantedMessageResponse> processLeaseGrantedMessage(LeaseGrantedMessage msg) {
        LOG.info("Received LeaseGrantedMessage for replica [groupId={}, leaseStartTime={}, force={}].", groupId, msg.leaseStartTime(),
                msg.force());

        return placementDriver.previousPrimaryExpired(groupId).thenCompose(unused -> leaderFuture()
                .thenComposeAsync(leader -> {
                    HybridTimestamp leaseExpirationTime = this.leaseExpirationTime;

                    assert leaseExpirationTime == null || clockService.after(msg.leaseExpirationTime(), leaseExpirationTime)
                            : "Invalid lease expiration time in message, msg=" + msg;

                    if (msg.force()) {
                        // Replica must wait till storage index reaches the current leader's index to make sure that all updates made on the
                        // group leader are received.
                        return waitForActualState(msg.leaseStartTime(), msg.leaseExpirationTime().getPhysical())
                                .thenCompose(v -> sendPrimaryReplicaChangeToReplicationGroup(
                                        msg.leaseStartTime().longValue(),
                                        localNode.id(),
                                        localNode.name()
                                ))
                                .thenCompose(v -> {
                                    CompletableFuture<LeaseGrantedMessageResponse> respFut =
                                            acceptLease(msg.leaseStartTime(), msg.leaseExpirationTime());

                                    if (leader.equals(localNode)) {
                                        return respFut;
                                    } else {
                                        return raftClient.transferLeadership(new Peer(localNode.name()))
                                                .thenCompose(ignored -> respFut);
                                    }
                                });
                    } else {
                        if (leader.equals(localNode)) {
                            return waitForActualState(msg.leaseStartTime(), msg.leaseExpirationTime().getPhysical())
                                    .thenCompose(v -> sendPrimaryReplicaChangeToReplicationGroup(
                                            msg.leaseStartTime().longValue(),
                                            localNode.id(),
                                            localNode.name()
                                    ))
                                    .thenCompose(v -> acceptLease(msg.leaseStartTime(), msg.leaseExpirationTime()));
                        } else {
                            return proposeLeaseRedirect(leader);
                        }
                    }
                }, executor)
        );
    }

    private CompletableFuture<Void> sendPrimaryReplicaChangeToReplicationGroup(
            long leaseStartTime,
            UUID primaryReplicaNodeId,
            String primaryReplicaNodeName
    ) {
        PrimaryReplicaChangeCommand cmd = REPLICA_MESSAGES_FACTORY.primaryReplicaChangeCommand()
                .leaseStartTime(leaseStartTime)
                .primaryReplicaNodeId(primaryReplicaNodeId)
                .primaryReplicaNodeName(primaryReplicaNodeName)
                .build();

        return raftClient.run(cmd);
    }

    private CompletableFuture<LeaseGrantedMessageResponse> acceptLease(
            HybridTimestamp leaseStartTime,
            HybridTimestamp leaseExpirationTime
    ) {
        LOG.info("Lease accepted [group=" + groupId + ", leaseStartTime=" + leaseStartTime + "].");

        this.leaseExpirationTime = leaseExpirationTime;

        LeaseGrantedMessageResponse resp = PLACEMENT_DRIVER_MESSAGES_FACTORY.leaseGrantedMessageResponse()
                .accepted(true)
                .build();

        return completedFuture(resp);
    }

    private CompletableFuture<LeaseGrantedMessageResponse> proposeLeaseRedirect(ClusterNode groupLeader) {
        LOG.info("Proposing lease redirection [groupId={}, proposed node={}].", groupId, groupLeader);

        LeaseGrantedMessageResponse resp = PLACEMENT_DRIVER_MESSAGES_FACTORY.leaseGrantedMessageResponse()
                .accepted(false)
                .redirectProposal(groupLeader.name())
                .build();

        return completedFuture(resp);
    }

    /**
     * Tries to read index from group leader and wait for this index to appear in local storage. Can possible return failed future with
     * timeout exception, and in this case, replica would not answer to placement driver, because the response is useless. Placement driver
     * should handle this.
     *
     * @param startTime Lease start time.
     * @param expirationTime Lease expiration time.
     * @return Future that is completed when local storage catches up the index that is actual for leader on the moment of request.
     */
    private CompletableFuture<Void> waitForActualState(HybridTimestamp startTime, long expirationTime) {
        LOG.info("Waiting for actual storage state, group=" + groupId);

        replicaReservationClosure.accept(groupId, startTime);

        long timeout = expirationTime - currentTimeMillis();
        if (timeout <= 0) {
            return failedFuture(new TimeoutException());
        }

        return retryOperationUntilSuccess(raftClient::readIndex, e -> currentTimeMillis() > expirationTime, executor)
                .orTimeout(timeout, TimeUnit.MILLISECONDS)
                .thenCompose(storageIndexTracker::waitFor);
    }

    private void onLeaderElected(ClusterNode clusterNode, long term) {
        leaderRef = clusterNode;
        leaderReadyFuture.complete(null);
    }

    private CompletableFuture<ClusterNode> leaderFuture() {
        return leaderReadyFuture.thenApply(ignored -> leaderRef);
    }
}
