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

package org.apache.ignite.internal.sql.engine.message;

import org.apache.ignite.internal.network.NetworkMessage;
import org.apache.ignite.internal.network.annotations.Marshallable;
import org.apache.ignite.internal.network.annotations.Transferable;
import org.jetbrains.annotations.Nullable;

/**
 * Contains the result of the remote cancel operation.
 *
 * @see CancelOperationRequest
 */
@Transferable(SqlQueryMessageGroup.OPERATION_CANCEL_RESPONSE)
public interface CancelOperationResponse extends NetworkMessage {
    /**
     * Returns the result of canceling an operation on the remote node.
     * If the result is {@code null} then {@link #error()} must not be {@code null}.
     *
     * @return Result of canceling an operation on the remote node, or {@code null} in case of error.
     */
    @Nullable Boolean result();

    /**
     * Returns the error that occurred when canceling an operation on the remote node.
     * If the {@code error} is {@code null} then {@link #result()} must not be {@code null}.
     *
     * @return Error that occurred when canceling an operation on the remote node., or {@code null} if there was no error.
     */
    @Marshallable
    @Nullable Throwable error();
}
