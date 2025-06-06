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

package org.apache.ignite.internal.sql.engine.exec.rel;

import static org.apache.ignite.internal.lang.IgniteStringFormatter.format;
import static org.apache.ignite.internal.util.CollectionUtils.nullOrEmpty;

import java.util.List;
import org.apache.ignite.internal.lang.Debuggable;
import org.apache.ignite.internal.lang.IgniteStringBuilder;
import org.apache.ignite.internal.lang.RunnableX;
import org.apache.ignite.internal.sql.engine.exec.ExecutionContext;
import org.apache.ignite.internal.sql.engine.util.Commons;
import org.apache.ignite.internal.util.IgniteUtils;
import org.jetbrains.annotations.TestOnly;

/**
 * Abstract node of execution tree.
 */
public abstract class AbstractNode<RowT> implements Node<RowT> {
    /** Special value to highlight that all row were received and we do not expect more. */
    static final int NOT_WAITING = -1;

    /** Batch size for DML operations. */
    public static final int MODIFY_BATCH_SIZE = 100;

    /** Batch size for network operations. */
    static final int IO_BATCH_SIZE = Commons.IO_BATCH_SIZE;

    /** Max count for parallel network requests. */
    static final int IO_BATCH_CNT = Commons.IO_BATCH_COUNT;

    /** Execution node buffer size. */
    protected final int inBufSize;

    private final ExecutionContext<RowT> ctx;

    /** For debug purpose. */
    private volatile Thread thread;

    private Downstream<RowT> downstream;

    private boolean closed;

    private List<Node<RowT>> sources;

    /**
     * Constructor.
     * TODO Documentation https://issues.apache.org/jira/browse/IGNITE-15859
     *
     * @param ctx Execution context.
     */
    protected AbstractNode(ExecutionContext<RowT> ctx) {
        this.ctx = ctx;
        this.inBufSize = ctx.bufferSize();
    }

    /** {@inheritDoc} */
    @Override
    public ExecutionContext<RowT> context() {
        return ctx;
    }

    /** {@inheritDoc} */
    @Override
    public void register(List<Node<RowT>> sources) {
        this.sources = sources;

        for (int i = 0; i < sources.size(); i++) {
            sources.get(i).onRegister(requestDownstream(i));
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Node<RowT>> sources() {
        return sources;
    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        if (isClosed()) {
            return;
        }

        closeInternal();

        if (!nullOrEmpty(sources())) {
            sources().forEach(Commons::closeQuiet);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void rewind() {
        rewindInternal();

        if (!nullOrEmpty(sources())) {
            sources().forEach(Node::rewind);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void execute(RunnableX task) {
        if (this.isClosed()) {
            return;
        }

        context().execute(() -> {
            // If the node is closed, the task must be ignored.
            if (this.isClosed()) {
                return;
            }

            checkState();

            task.run();
        }, this::onError);
    }

    /** {@inheritDoc} */
    @Override
    public void onRegister(Downstream<RowT> downstream) {
        this.downstream = downstream;
    }

    /**
     * Processes given exception.
     *
     * @param e Exception.
     */
    public void onError(Throwable e) {
        Downstream<RowT> downstream = downstream();

        assert downstream != null;

        try {
            downstream.onError(e);
        } finally {
            Commons.closeQuiet(this);
        }
    }

    protected void closeInternal() {
        closed = true;
    }

    protected abstract void rewindInternal();

    /**
     * Get closed flag: {@code true} if the subtree is canceled.
     */
    public boolean isClosed() {
        return closed;
    }

    void checkState() {
        if (!IgniteUtils.assertionsEnabled()) {
            return;
        }

        Thread currentedThread = Thread.currentThread();

        synchronized (this) {
            if (thread == null) {
                thread = currentedThread;
            } else {
                assert thread == currentedThread : format("expThread={}, actThread={}, "
                                + "executionId={}, fragmentId={}", thread.getName(), currentedThread.getName(),
                        context().executionId(), context().fragmentId());
            }
        }
    }

    protected abstract Downstream<RowT> requestDownstream(int idx);

    @Override
    public Downstream<RowT> downstream() {
        return downstream;
    }

    protected void dumpDebugInfo0(IgniteStringBuilder buf) {
        buf.app("class=").app(getClass().getSimpleName());
    }

    @Override
    @TestOnly
    public void dumpState(IgniteStringBuilder writer, String indent) {
        writer.app(indent);
        dumpDebugInfo0(writer);
        writer.nl();

        if (sources != null) {
            writer.app(indent).app("Sources: ").nl();
            Debuggable.dumpState(writer, Debuggable.childIndentation(indent), sources);
        }
    }
}
