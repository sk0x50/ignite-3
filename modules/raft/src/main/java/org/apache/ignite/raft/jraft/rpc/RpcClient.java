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
package org.apache.ignite.raft.jraft.rpc;

import org.apache.ignite.internal.network.TopologyEventHandler;
import org.apache.ignite.raft.jraft.Lifecycle;
import org.apache.ignite.raft.jraft.entity.PeerId;
import org.apache.ignite.raft.jraft.option.RpcOptions;

/**
 * RPC client.
 */
public interface RpcClient extends Lifecycle<RpcOptions>, NetworkInvoker {
    /**
     * Check connection for given address.
     *
     * @param peerId target peer ID.
     * @return true if there is a connection and the connection is active and writable.
     * @deprecated // TODO asch remove IGNITE-14832
     */
    boolean checkConnection(PeerId peerId);

    /**
     * Register a connect event listener for the handler.
     *
     * @param handler The handler.
     */
    void registerConnectEventListener(TopologyEventHandler handler);
}
