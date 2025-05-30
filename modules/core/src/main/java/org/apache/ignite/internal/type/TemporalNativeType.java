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

import static org.apache.ignite.internal.lang.IgniteStringFormatter.format;

import java.util.Objects;
import org.apache.ignite.internal.tostring.S;
import org.apache.ignite.sql.ColumnType;

/**
 * Temporal native type.
 */
public class TemporalNativeType extends NativeType {
    /**
     * Creates TIME type.
     *
     * @param precision Fractional seconds precision.
     * @return Native type.
     */
    static TemporalNativeType time(int precision) {
        int size = (precision > 3) ? 6 : 4;

        return new TemporalNativeType(ColumnType.TIME, size, precision);
    }

    /**
     * Creates DATETIME type.
     *
     * @param precision Fractional seconds precision.
     * @return Native type.
     */
    static TemporalNativeType datetime(int precision) {
        int size = NativeTypes.DATE.sizeInBytes() + ((precision > 3) ? 6 : 4);

        return new TemporalNativeType(ColumnType.DATETIME, size, precision);
    }

    /**
     * Creates TIMESTAMP type.
     *
     * @param precision Fractional seconds precision.
     * @return Native type.
     */
    static TemporalNativeType timestamp(int precision) {
        int size = (precision == 0) ? 8 : 12;

        return new TemporalNativeType(ColumnType.TIMESTAMP, size, precision);
    }

    /** Fractional seconds precision. */
    private final int precision;

    /**
     * Creates temporal type.
     *
     * @param typeSpec  Type spec.
     * @param precision Fractional seconds precision.
     */
    private TemporalNativeType(ColumnType typeSpec, int size, int precision) {
        super(typeSpec, size);

        if (precision < 0 || precision > NativeTypes.MAX_TIME_PRECISION) {
            throw new IllegalArgumentException("Unsupported fractional seconds precision: " + precision);
        }

        this.precision = precision;
    }

    /**
     * Return fractional seconds precision.
     *
     * @return Precision;
     */
    public int precision() {
        return precision;
    }

    /** {@inheritDoc} */
    @Override
    public String displayName() {
        return format("{}({})", super.displayName(), precision);
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
        if (!super.equals(o)) {
            return false;
        }
        TemporalNativeType that = (TemporalNativeType) o;
        return precision == that.precision;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), precision);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return S.toString(TemporalNativeType.class.getSimpleName(), "name", spec(), "precision", precision);
    }
}
