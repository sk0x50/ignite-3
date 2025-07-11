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

package org.apache.ignite.internal.tx.impl;

import java.util.Objects;
import java.util.Set;
import org.apache.ignite.internal.replicator.ReplicationGroupId;
import org.apache.ignite.internal.replicator.TablePartitionId;
import org.apache.ignite.internal.tostring.IgniteToStringInclude;
import org.apache.ignite.internal.tostring.S;

/**
 * Partition enlistment information together with partition group ID.
 */
public class EnlistedPartitionGroup {
    private final ReplicationGroupId groupId;

    @IgniteToStringInclude
    private final Set<Integer> tableIds;

    /** Constructor. */
    public EnlistedPartitionGroup(ReplicationGroupId groupId, Set<Integer> tableIds) {
        this.groupId = groupId;
        this.tableIds = Set.copyOf(tableIds);
    }

    /** Constructor. */
    public EnlistedPartitionGroup(TablePartitionId groupId) {
        this(groupId, Set.of(groupId.tableId()));
    }

    /**
     * Returns replication group ID of the partition.
     */
    public ReplicationGroupId groupId() {
        return groupId;
    }

    /**
     * Returns IDs of tables for which the partition is enlisted.
     */
    public Set<Integer> tableIds() {
        return tableIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EnlistedPartitionGroup that = (EnlistedPartitionGroup) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(tableIds, that.tableIds);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(groupId);
        result = 31 * result + Objects.hashCode(tableIds);
        return result;
    }

    @Override
    public String toString() {
        return S.toString(EnlistedPartitionGroup.class, this);
    }
}
