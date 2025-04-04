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
// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: cli.proto

package org.apache.ignite.raft.jraft.rpc;

import java.util.Collection;
import org.apache.ignite.internal.replicator.ReplicationGroupId;
import org.apache.ignite.internal.network.annotations.Marshallable;
import org.apache.ignite.internal.network.annotations.Transferable;
import org.apache.ignite.raft.jraft.RaftMessageGroup;
import org.apache.ignite.raft.jraft.RaftMessageGroup.RpcClientMessageGroup;
import org.jetbrains.annotations.Nullable;

public final class CliRequests {
    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.ADD_PEER_REQUEST)
    public interface AddPeerRequest extends Message {
        String groupId();

        String leaderId();

        String peerId();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.ADD_PEER_RESPONSE)
    public interface AddPeerResponse extends Message {
        @Nullable
        Collection<String> oldPeersList();

        Collection<String> newPeersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.REMOVE_PEER_REQUEST)
    public interface RemovePeerRequest extends Message {
        String groupId();

        String leaderId();

        String peerId();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.REMOVE_PEER_RESPONSE)
    public interface RemovePeerResponse extends Message {
        @Nullable
        Collection<String> oldPeersList();

        Collection<String> newPeersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.CHANGE_PEERS_AND_LEARNERS_REQUEST)
    public interface ChangePeersAndLearnersRequest extends Message {
        String groupId();

        String leaderId();

        Collection<String> newPeersList();

        Collection<String> newLearnersList();

        // term is intentionally Long and not long in order to perform nullable (not initialized) check.
        Long term();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.CHANGE_PEERS_AND_LEARNERS_RESPONSE)
    public interface ChangePeersAndLearnersResponse extends Message {
        @Nullable
        Collection<String> oldPeersList();

        Collection<String> newPeersList();

        @Nullable
        Collection<String> oldLearnersList();

        Collection<String> newLearnersList();
    }

    @Transferable(value = RpcClientMessageGroup.CHANGE_PEERS_AND_LEARNERS_ASYNC_REQUEST)
    public interface ChangePeersAndLearnersAsyncRequest extends Message {
        String groupId();

        String leaderId();

        Collection<String> newPeersList();

        Collection<String> newLearnersList();

        // term is intentionally Long and not long in order to perform nullable (not initialized) check.
        Long term();
    }

    @Transferable(value = RpcClientMessageGroup.CHANGE_PEERS_AND_LEARNERS_ASYNC_RESPONSE)
    public interface ChangePeersAndLearnersAsyncResponse extends Message {
        Collection<String> oldPeersList();

        Collection<String> newPeersList();

        Collection<String> oldLearnersList();

        Collection<String> newLearnersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.SNAPSHOT_REQUEST)
    public interface SnapshotRequest extends Message {
        String groupId();

        String peerId();

        boolean forced();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.RESET_PEER_REQUEST)
    public interface ResetPeerRequest extends Message {
        String groupId();

        String peerId();

        @Nullable
        Collection<String> oldPeersList();

        Collection<String> newPeersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.TRANSFER_LEADER_REQUEST)
    public interface TransferLeaderRequest extends Message {
        String groupId();

        String leaderId();

        String peerId();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.GET_LEADER_REQUEST)
    public interface GetLeaderRequest extends Message {
        String groupId();

        @Nullable
        String peerId();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.GET_LEADER_RESPONSE)
    public interface GetLeaderResponse extends Message {
        String leaderId();

        long currentTerm();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.GET_PEERS_REQUEST)
    public interface GetPeersRequest extends Message {
        String groupId();

        @Nullable
        String leaderId();

        boolean onlyAlive();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.GET_PEERS_RESPONSE)
    public interface GetPeersResponse extends Message {
        Collection<String> peersList();

        Collection<String> learnersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.ADD_LEARNERS_REQUEST)
    public interface AddLearnersRequest extends Message {
        String groupId();

        String leaderId();

        Collection<String> learnersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.REMOVE_LEARNERS_REQUEST)
    public interface RemoveLearnersRequest extends Message {
        String groupId();

        String leaderId();

        Collection<String> learnersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.RESET_LEARNERS_REQUEST)
    public interface ResetLearnersRequest extends Message {
        String groupId();

        String leaderId();

        Collection<String> learnersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.LEARNERS_OP_RESPONSE)
    public interface LearnersOpResponse extends Message {
        @Nullable
        Collection<String> oldLearnersList();

        Collection<String> newLearnersList();
    }

    @Transferable(value = RaftMessageGroup.RpcClientMessageGroup.SUBSCRIPTION_LEADER_CHANGE_REQUEST)
    public interface SubscriptionLeaderChangeRequest extends Message {
        @Marshallable
        ReplicationGroupId groupId();

        /**
        * Gets a subscription flag.
        *
        * @return True if subscription is started, false when it finished.
        */
        boolean subscribe();
    }

    @Transferable(value = RpcClientMessageGroup.SUBSCRIPTION_LEADER_CHANGE_REQUEST_ACKNOWLEDGE)
    public interface SubscriptionLeaderChangeRequestAcknowledge extends Message {
    }

    @Transferable(value = RpcClientMessageGroup.LEADER_CHANGE_NOTIFICATION)
    public interface LeaderChangeNotification extends Message {
        long term();

        @Marshallable
        ReplicationGroupId groupId();
    }
}
