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

package org.apache.ignite.internal.metrics.message;

import static org.apache.ignite.internal.metrics.messaging.MetricMessageTypes.METRIC_DISABLE_RESPONSE;

import org.apache.ignite.internal.network.NetworkMessage;
import org.apache.ignite.internal.network.annotations.Marshallable;
import org.apache.ignite.internal.network.annotations.Transferable;
import org.jetbrains.annotations.Nullable;

/** Remote metric source disable response. */
@Transferable(METRIC_DISABLE_RESPONSE)
public interface MetricDisableResponse extends NetworkMessage {
    /**
     * Returns a {@link Throwable} that was thrown during disabling the metric source ({@code null} if the request was successful).
     *
     * @return {@link Throwable} that was thrown during disabling the metric source ({@code null} if the request was successful).
     */
    @Nullable
    @Marshallable
    Throwable throwable();
}
