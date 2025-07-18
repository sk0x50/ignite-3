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
package org.apache.ignite.raft.jraft.util.concurrent;

import io.netty.util.concurrent.DefaultEventExecutor;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.ignite.internal.logger.IgniteLogger;
import org.apache.ignite.internal.logger.Loggers;
import org.apache.ignite.internal.thread.IgniteThreadFactory;
import org.apache.ignite.raft.jraft.util.ExecutorServiceHelper;
import org.apache.ignite.raft.jraft.util.ThreadPoolUtil;
import org.jctools.queues.MpscBlockingConsumerArrayQueue;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 *
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SingleThreadExecutorBenchmark {
    private static final IgniteLogger LOG = Loggers.forClass(SingleThreadExecutorBenchmark.class);

    private static final int TIMES = 1000000;
    private static final int THREADS = 32;

    private ExecutorService producers;

    /*
     * Benchmark                                                                         Mode  Cnt  Score   Error  Units
     * SingleThreadExecutorBenchmark.defaultSingleThreadPollExecutor                    thrpt    3  1.266 ± 2.822  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutor                           thrpt    3  4.066 ± 4.990  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutorWithConcurrentLinkedQueue  thrpt    3  3.470 ± 0.845  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutorWithLinkedBlockingQueue    thrpt    3  2.643 ± 1.222  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutorWithLinkedTransferQueue    thrpt    3  3.266 ± 1.613  ops/s
     * SingleThreadExecutorBenchmark.nettyDefaultEventExecutor                          thrpt    3  2.290 ± 0.446  ops/s
     *
     * Benchmark                                                                         Mode  Cnt  Score   Error  Units
     * SingleThreadExecutorBenchmark.defaultSingleThreadPollExecutor                    thrpt   10  1.389 ± 0.130  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutor                           thrpt   10  3.646 ± 0.323  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutorWithConcurrentLinkedQueue  thrpt   10  3.386 ± 0.247  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutorWithLinkedBlockingQueue    thrpt   10  2.535 ± 0.153  ops/s
     * SingleThreadExecutorBenchmark.mpscSingleThreadExecutorWithLinkedTransferQueue    thrpt   10  3.184 ± 0.299  ops/s
     * SingleThreadExecutorBenchmark.nettyDefaultEventExecutor                          thrpt   10  2.097 ± 0.075  ops/s
     */

    public static void main(String[] args) throws RunnerException {
        final Options opt = new OptionsBuilder() //
            .include(SingleThreadExecutorBenchmark.class.getSimpleName()) //
            .warmupIterations(3) //
            .measurementIterations(10) //
            .forks(1) //
            .build();

        new Runner(opt).run();
    }

    @Setup
    public void setup() {
        this.producers = newProducers();
    }

    @TearDown
    public void tearDown() {
        ExecutorServiceHelper.shutdownAndAwaitTermination(this.producers);
    }

    @Benchmark
    public void nettyDefaultEventExecutor() throws InterruptedException {
        execute(new DefaultSingleThreadExecutor(
            new DefaultEventExecutor(IgniteThreadFactory.create("node", "netty_executor", true, LOG))));
    }

    @Benchmark
    public void defaultSingleThreadPollExecutor() throws InterruptedException {
        execute(new DefaultSingleThreadExecutor("default", TIMES));
    }

    @Benchmark
    public void defaultSingleThreadPollExecutorWithMpscBlockingQueue() throws InterruptedException {
        ThreadPoolExecutor pool = ThreadPoolUtil.newBuilder() //
            .coreThreads(1) //
            .maximumThreads(1) //
            .poolName("default") //
            .enableMetric(false) //
            .workQueue(new MpscBlockingConsumerArrayQueue<>(TIMES)) // TODO asch IGNITE-15997
            .keepAliveSeconds(60L) //
            .threadFactory(IgniteThreadFactory.create("node", "default", true, LOG)) //
            .build();

        execute(new DefaultSingleThreadExecutor(pool));
    }

    @Benchmark
    public void mpscSingleThreadExecutor() throws InterruptedException {
        execute(new MpscSingleThreadExecutor(TIMES, IgniteThreadFactory.create("node", "mpsc", true, LOG)));
    }

    @Benchmark
    public void mpscSingleThreadExecutorWithConcurrentLinkedQueue() throws InterruptedException {
        execute(new MpscSingleThreadExecutor(TIMES, IgniteThreadFactory.create("node", "mpsc_clq", true, LOG)) {

            @Override
            protected Queue<Runnable> newTaskQueue(final int maxPendingTasks) {
                return new ConcurrentLinkedQueue<>();
            }
        });
    }

    @Benchmark
    public void mpscSingleThreadExecutorWithLinkedBlockingQueue() throws InterruptedException {
        execute(new MpscSingleThreadExecutor(TIMES, IgniteThreadFactory.create("node", "mpsc_lbq", true, LOG)) {

            @Override
            protected Queue<Runnable> newTaskQueue(final int maxPendingTasks) {
                return new LinkedBlockingQueue<>(maxPendingTasks);
            }
        });
    }

    @Benchmark
    public void mpscSingleThreadExecutorWithLinkedTransferQueue() throws InterruptedException {
        execute(new MpscSingleThreadExecutor(TIMES, IgniteThreadFactory.create("node", "mpsc_ltq", true, LOG)) {

            @Override
            protected Queue<Runnable> newTaskQueue(final int maxPendingTasks) {
                return new LinkedTransferQueue<>();
            }
        });
    }

    private void execute(final SingleThreadExecutor executor) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(TIMES);
        for (int i = 0; i < TIMES; i++) {
            this.producers.execute(() -> executor.execute(latch::countDown));
        }
        latch.await();
    }

    private static ExecutorService newProducers() {
        return ThreadPoolUtil.newBuilder() //
            .coreThreads(THREADS) //
            .maximumThreads(THREADS) //
            .poolName("benchmark") //
            .enableMetric(false) //
            .workQueue(new ArrayBlockingQueue<>(TIMES)) //
            .keepAliveSeconds(60L) //
            .threadFactory(IgniteThreadFactory.create("node", "benchmark", true, LOG)) //
            .build();
    }
}
