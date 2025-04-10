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

package org.apache.ignite.internal.catalog.storage;

import java.util.List;
import java.util.Objects;
import org.apache.ignite.internal.catalog.storage.serialization.MarshallableEntryType;
import org.apache.ignite.internal.tostring.IgniteToStringInclude;
import org.apache.ignite.internal.tostring.S;

/**
 * Group of changes that relates to specified version.
 */
public class VersionedUpdate implements UpdateLogEvent {
    private final int version;

    private final long delayDurationMs;

    @IgniteToStringInclude
    private final List<UpdateEntry> entries;

    /**
     * Constructs the object.
     *
     * @param version A version the changes relate to.
     * @param delayDurationMs Delay duration that, when added to the update's entry timestamp assigned by the MetaStorage, will produce the
     *     activation timestamp (milliseconds).
     * @param entries A list of changes.
     */
    public VersionedUpdate(int version, long delayDurationMs, List<UpdateEntry> entries) {
        this.version = version;
        this.delayDurationMs = delayDurationMs;
        this.entries = List.copyOf(
                Objects.requireNonNull(entries, "entries")
        );
    }

    /** Returns version. */
    public int version() {
        return version;
    }

    /** Returns Delay Duration for this update (in milliseconds). */
    public long delayDurationMs() {
        return delayDurationMs;
    }

    /** Returns list of changes. */
    public List<UpdateEntry> entries() {
        return entries;
    }

    @Override
    public int typeId() {
        return MarshallableEntryType.VERSIONED_UPDATE.id();
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

        VersionedUpdate that = (VersionedUpdate) o;

        return version == that.version && entries.equals(that.entries);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = version;
        result = 31 * result + entries.hashCode();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return S.toString(this);
    }
}
