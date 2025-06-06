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
package org.apache.ignite.raft.jraft.rpc.impl;

import static org.apache.ignite.internal.util.CompletableFutures.falseCompletedFuture;
import static org.apache.ignite.internal.util.CompletableFutures.trueCompletedFuture;

import java.net.ConnectException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import org.apache.ignite.internal.lang.NodeStoppingException;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.internal.network.TopologyEventHandler;
import org.apache.ignite.internal.raft.PeerUnavailableException;
import org.apache.ignite.internal.util.ExceptionUtils;
import org.apache.ignite.network.ClusterNode;
import org.apache.ignite.raft.jraft.Status;
import org.apache.ignite.raft.jraft.entity.PeerId;
import org.apache.ignite.raft.jraft.error.InvokeTimeoutException;
import org.apache.ignite.raft.jraft.error.RaftError;
import org.apache.ignite.raft.jraft.error.RemotingException;
import org.apache.ignite.raft.jraft.option.RpcOptions;
import org.apache.ignite.raft.jraft.rpc.ClientService;
import org.apache.ignite.raft.jraft.rpc.InvokeCallback;
import org.apache.ignite.raft.jraft.rpc.InvokeContext;
import org.apache.ignite.raft.jraft.rpc.Message;
import org.apache.ignite.raft.jraft.rpc.NetworkInvoker;
import org.apache.ignite.raft.jraft.rpc.RpcClient;
import org.apache.ignite.raft.jraft.rpc.RpcRequests;
import org.apache.ignite.raft.jraft.rpc.RpcRequests.ErrorResponse;
import org.apache.ignite.raft.jraft.rpc.RpcResponseClosure;
import org.apache.ignite.raft.jraft.util.Utils;

/**
 * Abstract RPC client service based.
 */
public abstract class AbstractClientService implements ClientService, TopologyEventHandler {
    protected static final IgniteLogger LOG = Loggers.forClass(AbstractClientService.class);

    protected volatile RpcClient rpcClient;
    protected ExecutorService rpcExecutor;
    protected RpcOptions rpcOptions;

    private Set<PeerId> deadPeers = ConcurrentHashMap.newKeySet();

    /**
     * The set of pinged consistent IDs.
     */
    private final Set<String> readyConsistentIds = ConcurrentHashMap.newKeySet();

    public RpcClient getRpcClient() {
        return this.rpcClient;
    }

    @Override
    public synchronized boolean init(final RpcOptions rpcOptions) {
        if (this.rpcClient != null) {
            return true;
        }
        this.rpcOptions = rpcOptions;
        return initRpcClient(this.rpcOptions.getRpcProcessorThreadPoolSize());
    }

    @Override public void onAppeared(ClusterNode member) {
        // TODO https://issues.apache.org/jira/browse/IGNITE-14837
        // Perhaps, We can remove checking for dead nodes and replace it with SWIM node alive event
        // and start replicator when the event is received.

        // No-op.
    }

    @Override public void onDisappeared(ClusterNode member) {
        readyConsistentIds.remove(member.name());
    }

    protected void configRpcClient(final RpcClient rpcClient) {
        rpcClient.registerConnectEventListener(this);
    }

    protected boolean initRpcClient(final int rpcProcessorThreadPoolSize) {
        this.rpcClient = rpcOptions.getRpcClient();

        configRpcClient(this.rpcClient);

        // TODO asch should the client be created lazily? A client doesn't make sence without a server IGNITE-14832
        this.rpcClient.init(this.rpcOptions);

        this.rpcExecutor = rpcOptions.getClientExecutor();

        return true;
    }

    @Override
    public synchronized void shutdown() {
        if (this.rpcClient != null) {
            this.rpcClient.shutdown();
            this.rpcClient = null;
        }
    }

    @Override
    public boolean connect(final PeerId peerId) {
        try {
            return connectAsync(peerId).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            LOG.error("Interrupted while connecting to {}, exception: {}.", peerId, e.getMessage());
        } catch (ExecutionException e) {
            if (!deadPeers.contains(peerId)) {
                deadPeers.add(peerId);

                LOG.error("Fail to connect {}, exception: {}.", peerId, e.getMessage());
            }
        }

        return false;
    }

    @Override
    public CompletableFuture<Boolean> connectAsync(PeerId peerId) {
        final RpcClient rc = this.rpcClient;
        if (rc == null) {
            return falseCompletedFuture();
        }

        // Remote node is alive and pinged, safe to continue.
        if (readyConsistentIds.contains(peerId.getConsistentId())) {
            return trueCompletedFuture();
        }

        final RpcRequests.PingRequest req = rpcOptions.getRaftMessagesFactory()
                .pingRequest()
                .sendTimestamp(System.currentTimeMillis())
                .build();

        CompletableFuture<Message> fut =
                invokeWithDone(peerId, req, null, null, rpcOptions.getRpcConnectTimeoutMs(), rpcExecutor, this.rpcClient);

        return fut.thenApply(msg -> {
            ErrorResponse resp = (ErrorResponse) msg;

            if (resp != null && resp.errorCode() == 0) {
                readyConsistentIds.add(peerId.getConsistentId());

                deadPeers.remove(peerId);

                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public <T extends Message> CompletableFuture<Message> invokeWithDone(final PeerId peerId, final Message request,
        final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(peerId, request, done, timeoutMs, this.rpcExecutor);
    }

    public <T extends Message> CompletableFuture<Message> invokeWithDone(final PeerId peerId, final Message request,
        final RpcResponseClosure<T> done, final int timeoutMs,
        final Executor rpcExecutor) {
        return invokeWithDone(peerId, request, null, done, timeoutMs, rpcExecutor, this.rpcClient);
    }

    public <T extends Message> CompletableFuture<Message> invokeWithDone(final PeerId peerId, final Message request,
        final InvokeContext ctx,
        final RpcResponseClosure<T> done, final int timeoutMs) {
        return invokeWithDone(peerId, request, ctx, done, timeoutMs, this.rpcExecutor, this.rpcClient);
    }

    public <T extends Message> CompletableFuture<Message> invokeWithDone(final PeerId peerId, final Message request,
        final InvokeContext ctx,
        final RpcResponseClosure<T> done, final int timeoutMs,
        final Executor rpcExecutor, NetworkInvoker rc) {
        final FutureImpl<Message> future = new FutureImpl<>();
        final Executor currExecutor = rpcExecutor != null ? rpcExecutor : this.rpcExecutor;

        try {
            if (rc == null) {
                // TODO asch replace with ignite exception, check all places IGNITE-14832
                future.completeExceptionally(new IllegalStateException("Client service is uninitialized."));
                // should be in another thread to avoid dead locking.
                Utils.runClosureInExecutor(currExecutor, done, new Status(RaftError.EINTERNAL,
                    "Client service is uninitialized."));
                return future;
            }

            return rc.invokeAsync(peerId, request, ctx, new InvokeCallback() {
                @Override
                public void complete(final Object result, final Throwable err) {
                    if (err == null) {
                        Status status = Status.OK();
                        Message msg;
                        if (result instanceof ErrorResponse) {
                            status = handleErrorResponse((ErrorResponse) result);
                            msg = (Message) result;
                        }
                        else {
                            msg = (Message) result;
                        }
                        if (done != null) {
                            try {
                                if (status.isOk()) {
                                    done.setResponse((T) msg);
                                }
                                done.run(status);
                            }
                            catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", t, request);
                            }
                        }
                        if (!future.isDone()) {
                            future.complete(msg);
                        }
                    }
                    else {
                        if (ExceptionUtils.hasCauseOrSuppressed(err, PeerUnavailableException.class, ConnectException.class))
                            readyConsistentIds.remove(peerId.getConsistentId()); // Force logical reconnect.

                        if (done != null) {
                            try {
                                done.run(new Status(errorCodeByException(err), "RPC exception:" + err.getMessage()));
                            }
                            catch (final Throwable t) {
                                LOG.error("Fail to run RpcResponseClosure, the request is {}.", t, request);
                            }
                        }
                        if (!future.isDone()) {
                            future.completeExceptionally(err);
                        }
                    }
                }

                @Override
                public Executor executor() {
                    return currExecutor;
                }
            }, timeoutMs <= 0 ? this.rpcOptions.getRpcDefaultTimeout() : timeoutMs);
        }
        catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            future.completeExceptionally(e);
            // should be in another thread to avoid dead locking.
            Utils.runClosureInExecutor(currExecutor, done,
                new Status(RaftError.EINTR, "Sending rpc was interrupted"));
        }
        catch (final RemotingException e) {
            future.completeExceptionally(e);
            // should be in another thread to avoid dead locking.
            Utils.runClosureInExecutor(currExecutor, done, new Status(RaftError.EINTERNAL,
                "Fail to send a RPC request:" + e.getMessage()));
        }

        return future;
    }

    private static RaftError errorCodeByException(Throwable err) {
        if (ExceptionUtils.hasCauseOrSuppressed(err, NodeStoppingException.class)) {
            return RaftError.ESHUTDOWN;
        }

        return err instanceof InvokeTimeoutException ? RaftError.ETIMEDOUT : RaftError.EINTERNAL;
    }

    private static Status handleErrorResponse(final ErrorResponse eResp) {
        final Status status = new Status();
        status.setCode(eResp.errorCode());
        status.setErrorMsg(eResp.errorMsg());
        return status;
    }
}
