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

package org.apache.ignite.lang;

import static org.apache.ignite.lang.ErrorGroups.Table.COLUMN_ALREADY_EXISTS_ERR;
import static org.apache.ignite.lang.util.IgniteNameUtils.quoteIfNeeded;

import java.util.UUID;

/**
 * This exception is thrown when a column creation has failed because a column with the specified name already existed.
 *
 * @deprecated No longer used. Use {@link org.apache.ignite.sql.SqlException} with {@link ErrorGroups.Sql#STMT_VALIDATION_ERR} error code.
 */
@Deprecated
public class ColumnAlreadyExistsException extends IgniteException {
    /**
     * Creates a new exception with a given column name.
     *
     * @param name Column name.
     */
    public ColumnAlreadyExistsException(String name) {
        super(COLUMN_ALREADY_EXISTS_ERR, "Column already exists [name=" + quoteIfNeeded(name) + ']');
    }

    /**
     * Creates a new exception with the given trace ID, error code, detailed message, and cause.
     *
     * @param traceId Unique identifier of the exception.
     * @param code Full error code.
     * @param message Detailed message.
     * @param cause Optional nested exception (can be {@code null}).
     */
    public ColumnAlreadyExistsException(UUID traceId, int code, String message, Throwable cause) {
        super(traceId, code, message, cause);
    }
}
