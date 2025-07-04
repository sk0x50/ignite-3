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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.ignite.Ignite;
import org.apache.ignite.compute.JobExecutionContext;
import org.apache.ignite.internal.testframework.BaseIgniteAbstractTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobExecutionContextImplTest extends BaseIgniteAbstractTest {
    @Mock
    private Ignite ignite;

    @Test
    void returnsIgnite() {
        JobExecutionContext context = new JobExecutionContextImpl(ignite, new AtomicBoolean(), null, null);

        assertThat(context.ignite(), is(sameInstance(ignite)));
    }

    @Test
    void returnsInterruptedFlag() {
        AtomicBoolean isInterrupted = new AtomicBoolean();

        JobExecutionContext context = new JobExecutionContextImpl(ignite, isInterrupted, null, null);

        assertThat(context.isCancelled(), is(false));

        isInterrupted.set(true);

        assertThat(context.isCancelled(), is(true));
    }
}
