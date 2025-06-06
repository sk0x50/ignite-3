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

package org.apache.ignite.internal.type;

import java.util.Arrays;
import org.apache.ignite.internal.tostring.S;
import org.apache.ignite.sql.ColumnType;
import org.jetbrains.annotations.TestOnly;

/**
 * A thin wrapper over {@link ColumnType} to instantiate parameterized constrained types.
 */
public class NativeType implements Comparable<NativeType> {
    private final ColumnType typeSpec;

    /** Type size in bytes. */
    private final int size;

    /** Flag indicating whether this type specifies a fixed-length type. */
    private final boolean fixedSize;

    /**
     * Constructor for fixed-length types.
     *
     * @param typeSpec Type spec.
     * @param size     Type size in bytes.
     */
    protected NativeType(ColumnType typeSpec, int size) {
        this.fixedSize = !((typeSpec.precisionAllowed() && typeSpec.scaleAllowed()) || typeSpec.lengthAllowed());

        if (!this.fixedSize) {
            throw new IllegalArgumentException("Size must be provided only for fixed-length types: " + typeSpec);
        }

        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive [typeSpec=" + typeSpec + ", size=" + size + ']');
        }

        this.typeSpec = typeSpec;
        this.size = size;
    }

    /**
     * Constructor for variable-length types.
     *
     * @param typeSpec Type spec.
     */
    protected NativeType(ColumnType typeSpec) {
        this.fixedSize = !((typeSpec.precisionAllowed() && typeSpec.scaleAllowed()) || typeSpec.lengthAllowed());

        if (this.fixedSize) {
            throw new IllegalArgumentException("Fixed-length types must be created by the "
                    + "length-aware constructor: " + typeSpec);
        }

        this.typeSpec = typeSpec;
        this.size = 0;
    }

    /**
     * Get fixed length flag: {@code true} for fixed-length types, {@code false} otherwise.
     */
    public boolean fixedLength() {
        return fixedSize;
    }

    /**
     * Get size in bytes of the type if it is a fixlen type. For varlen types the return value is undefined, so the user
     * should explicitly check {@code fixedLength()} before using this method.
     */
    public int sizeInBytes() {
        return size;
    }

    /**
     * Get type specification enum.
     */
    public ColumnType spec() {
        return typeSpec;
    }

    /**
     * Checks type mismatch.
     *
     * @param type Native type.
     * @return {@code true} if type or typeSpec doesn't match given one, {@code false} otherwise.
     */
    public boolean mismatch(NativeType type) {
        return this != type && typeSpec != type.typeSpec;
    }

    /**
     * Return human readable name of this type.
     *
     * @return Human readable name of this type.
     */
    public String displayName() {
        return typeSpec.name();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NativeType that = (NativeType) o;

        return size == that.size && typeSpec == that.typeSpec;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int res = typeSpec.hashCode();

        res = 31 * res + size;

        return res;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(NativeType o) {
        // Fixed-sized types go first.
        if (size <= 0 && o.size > 0) {
            return 1;
        }

        if (size > 0 && o.size <= 0) {
            return -1;
        }

        // Either size is -1 for both, or positive for both. Compare sizes, then description.
        int cmp = Integer.compare(size, o.size);

        if (cmp != 0) {
            return cmp;
        }

        return typeSpec.name().compareTo(o.typeSpec.name());
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return S.toString(NativeType.class.getSimpleName(),
                "name", typeSpec.name(),
                "sizeInBytes", size,
                "fixed", fixedLength());
    }


    // TODO https://issues.apache.org/jira/browse/IGNITE-17373 Remove filter after this issue is resolved
    @TestOnly
    public static ColumnType[] nativeTypes() {
        return Arrays.stream(ColumnType.values()).filter(v -> v != ColumnType.NULL && v != ColumnType.DURATION
                && v != ColumnType.PERIOD).toArray(ColumnType[]::new);
    }
}
