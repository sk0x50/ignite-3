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

package org.apache.ignite.distributed;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.Flow.Publisher;
import org.apache.ignite.internal.hlc.HybridTimestampTracker;
import org.apache.ignite.internal.lang.IgniteSystemProperties;
import org.apache.ignite.internal.replicator.PartitionGroupId;
import org.apache.ignite.internal.replicator.ReplicationGroupId;
import org.apache.ignite.internal.replicator.TablePartitionId;
import org.apache.ignite.internal.schema.BinaryRow;
import org.apache.ignite.internal.table.InternalTable;
import org.apache.ignite.internal.table.RollbackTxOnErrorPublisher;
import org.apache.ignite.internal.testframework.WithSystemProperty;
import org.apache.ignite.internal.tx.InternalTransaction;
import org.apache.ignite.internal.tx.InternalTxOptions;
import org.apache.ignite.internal.tx.PendingTxPartitionEnlistment;
import org.apache.ignite.internal.utils.PrimaryReplica;
import org.apache.ignite.network.ClusterNode;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InternalTable#scan(int, InternalTransaction)}.
 */
@WithSystemProperty(key = IgniteSystemProperties.COLOCATION_FEATURE_FLAG, value = "false")
public class ItInternalTableReadWriteScanTest extends ItAbstractInternalTableScanTest {
    /** Timestamp tracker. */
    private static final HybridTimestampTracker HYBRID_TIMESTAMP_TRACKER = HybridTimestampTracker.atomicTracker(null);

    @Override
    protected Publisher<BinaryRow> scan(int part, @Nullable InternalTransaction tx) {
        if (tx == null) {
            return internalTbl.scan(part, null);
        }

        PendingTxPartitionEnlistment enlistment =
                tx.enlistedPartition(targetReplicationGroupId(zoneId, part));

        PrimaryReplica recipient = new PrimaryReplica(
                clusterNodeResolver.getByConsistentId(enlistment.primaryNodeConsistentId()),
                enlistment.consistencyToken()
        );

        return new RollbackTxOnErrorPublisher<>(
                tx,
                internalTbl.scan(part, tx.id(), tx.commitPartition(), tx.coordinatorId(), recipient, null, null, null, 0, null)
        );
    }

    @Test
    public void testInvalidPartitionParameterScan() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scan(-1, null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> scan(1, null)
        );
    }

    @Override
    protected InternalTransaction startTx() {
        InternalTransaction tx = internalTbl.txManager().beginExplicitRw(HYBRID_TIMESTAMP_TRACKER, InternalTxOptions.defaults());

        int partId = ((PartitionGroupId) internalTbl.groupId()).partitionId();
        ReplicationGroupId tblPartId = targetReplicationGroupId(zoneId, partId);

        long term = 1L;

        tx.assignCommitPartition(tblPartId);

        ClusterNode primaryReplicaNode = getPrimaryReplica(tblPartId);

        tx.enlist(tblPartId, internalTbl.tableId(), primaryReplicaNode.name(), term);

        return tx;
    }

    // TODO: IGNITE-22522 - inline this after switching to ZonePartitionId.
    ReplicationGroupId targetReplicationGroupId(int tableOrZoneId, int partId) {
        return new TablePartitionId(tableOrZoneId, partId);
    }
}
