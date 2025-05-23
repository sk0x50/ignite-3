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

package org.apache.ignite.internal.tx.storage.state;

import org.apache.ignite.internal.lang.IgniteInternalException;
import org.apache.ignite.internal.lang.IgniteStringFormatter;
import org.apache.ignite.lang.ErrorGroups.Common;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown by the storage.
 */
public class TxStateStorageException extends IgniteInternalException {
    private static final long serialVersionUID = 8705275268121031742L;

    /**
     * Constructor.
     *
     * @param message Error message.
     */
    public TxStateStorageException(String message) {
        super(Common.INTERNAL_ERR, message);
    }

    /**
     * Constructor.
     *
     * @param message Error message.
     * @param cause Optional nested exception (can be {@code null}).
     */
    public TxStateStorageException(String message, @Nullable Throwable cause) {
        super(Common.INTERNAL_ERR, message, cause);
    }

    /**
     * Constructor.
     *
     * @param cause Non-null throwable cause.
     */
    public TxStateStorageException(Throwable cause) {
        super(Common.INTERNAL_ERR, cause);
    }

    public TxStateStorageException(int code, @Nullable Throwable cause) {
        super(code, cause);
    }

    /**
     * Constructor.
     *
     * @param code Full error code.
     * @param message Error message.
     */
    protected TxStateStorageException(int code, String message) {
        super(code, message);
    }

    /**
     * Constructor.
     *
     * @param code Full error code.
     * @param message Error message.
     * @param cause Optional nested exception (can be {@code null}).
     */
    protected TxStateStorageException(int code, String message, @Nullable Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Constructor.
     *
     * @param messagePattern Error message pattern.
     * @param cause Non-null throwable cause.
     * @param params Error message params.
     * @see IgniteStringFormatter#format(String, Object...)
     */
    public TxStateStorageException(String messagePattern, Throwable cause, Object... params) {
        this(IgniteStringFormatter.format(messagePattern, params), cause);
    }

    /**
     * Constructor.
     *
     * @param code Full error code.
     * @param messagePattern Error message pattern.
     * @param cause Non-null throwable cause.
     * @param params Error message params.
     * @see IgniteStringFormatter#format(String, Object...)
     */
    public TxStateStorageException(int code, String messagePattern, Throwable cause, Object... params) {
        this(code, IgniteStringFormatter.format(messagePattern, params), cause);
    }

    /**
     * Constructor.
     *
     * @param messagePattern Error message pattern.
     * @param params Error message params.
     * @see IgniteStringFormatter#format(String, Object...)
     */
    public TxStateStorageException(String messagePattern, Object... params) {
        this(IgniteStringFormatter.format(messagePattern, params));
    }

    /**
     * Constructor.
     *
     * @param code Full error code.
     * @param messagePattern Error message pattern.
     * @param params Error message params.
     * @see IgniteStringFormatter#format(String, Object...)
     */
    public TxStateStorageException(int code, String messagePattern, Object... params) {
        this(code, IgniteStringFormatter.format(messagePattern, params));
    }
}
