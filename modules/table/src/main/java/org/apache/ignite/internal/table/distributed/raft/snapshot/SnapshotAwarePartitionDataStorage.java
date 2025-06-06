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

package org.apache.ignite.internal.table.distributed.raft.snapshot;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.apache.ignite.internal.hlc.HybridTimestamp;
import org.apache.ignite.internal.partition.replicator.raft.snapshot.PartitionDataStorage;
import org.apache.ignite.internal.partition.replicator.raft.snapshot.PartitionKey;
import org.apache.ignite.internal.partition.replicator.raft.snapshot.ZonePartitionKey;
import org.apache.ignite.internal.partition.replicator.raft.snapshot.outgoing.OutgoingSnapshot;
import org.apache.ignite.internal.partition.replicator.raft.snapshot.outgoing.PartitionSnapshots;
import org.apache.ignite.internal.partition.replicator.raft.snapshot.outgoing.PartitionsSnapshots;
import org.apache.ignite.internal.raft.RaftGroupConfiguration;
import org.apache.ignite.internal.raft.RaftGroupConfigurationConverter;
import org.apache.ignite.internal.schema.BinaryRow;
import org.apache.ignite.internal.storage.AbortResult;
import org.apache.ignite.internal.storage.AddWriteCommittedResult;
import org.apache.ignite.internal.storage.AddWriteResult;
import org.apache.ignite.internal.storage.CommitResult;
import org.apache.ignite.internal.storage.MvPartitionStorage;
import org.apache.ignite.internal.storage.MvPartitionStorage.WriteClosure;
import org.apache.ignite.internal.storage.PartitionTimestampCursor;
import org.apache.ignite.internal.storage.ReadResult;
import org.apache.ignite.internal.storage.RowId;
import org.apache.ignite.internal.storage.StorageException;
import org.apache.ignite.internal.storage.gc.GcEntry;
import org.apache.ignite.internal.storage.lease.LeaseInfo;
import org.apache.ignite.internal.util.Cursor;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

/**
 * {@link PartitionDataStorage} that adds snapshot awareness. This means that MV writes coordinate with ongoing
 * snapshots to make sure that the writes do not interfere with the snapshots.
 */
public class SnapshotAwarePartitionDataStorage implements PartitionDataStorage {
    private final int tableId;
    private final MvPartitionStorage partitionStorage;
    private final PartitionsSnapshots partitionsSnapshots;
    private final PartitionKey partitionKey;

    private final RaftGroupConfigurationConverter raftGroupConfigurationConverter = new RaftGroupConfigurationConverter();

    /**
     * Creates a new instance.
     */
    public SnapshotAwarePartitionDataStorage(
            int tableId,
            MvPartitionStorage partitionStorage,
            PartitionsSnapshots partitionsSnapshots,
            PartitionKey partitionKey
    ) {
        this.tableId = tableId;
        this.partitionStorage = partitionStorage;
        this.partitionsSnapshots = partitionsSnapshots;
        this.partitionKey = partitionKey;
    }

    @Override
    public int tableId() {
        return tableId;
    }

    @Override
    public int partitionId() {
        return partitionKey.partitionId();
    }

    @Override
    public <V> V runConsistently(WriteClosure<V> closure) throws StorageException {
        return partitionStorage.runConsistently(closure);
    }

    @Override
    public void acquirePartitionSnapshotsReadLock() {
        PartitionSnapshots partitionSnapshots = getPartitionSnapshots();

        partitionSnapshots.acquireReadLock();
    }

    @Override
    public void releasePartitionSnapshotsReadLock() {
        PartitionSnapshots partitionSnapshots = getPartitionSnapshots();

        partitionSnapshots.releaseReadLock();
    }

    private PartitionSnapshots getPartitionSnapshots() {
        return partitionsSnapshots.partitionSnapshots(partitionKey);
    }

    @Override
    public CompletableFuture<Void> flush(boolean trigger) {
        return partitionStorage.flush(trigger);
    }

    @Override
    public long lastAppliedIndex() {
        return partitionStorage.lastAppliedIndex();
    }

    @Override
    public long lastAppliedTerm() {
        return partitionStorage.lastAppliedTerm();
    }

    @Override
    public void lastApplied(long lastAppliedIndex, long lastAppliedTerm) throws StorageException {
        partitionStorage.lastApplied(lastAppliedIndex, lastAppliedTerm);
    }

    @Override
    public void committedGroupConfiguration(RaftGroupConfiguration config) {
        partitionStorage.committedGroupConfiguration(raftGroupConfigurationConverter.toBytes(config));
    }

    @Override
    public @Nullable RaftGroupConfiguration committedGroupConfiguration() {
        return raftGroupConfigurationConverter.fromBytes(partitionStorage.committedGroupConfiguration());
    }

    @Override
    public AddWriteResult addWrite(
            RowId rowId,
            @Nullable BinaryRow row,
            UUID txId,
            int commitTableOrZoneId,
            int commitPartitionId
    ) throws StorageException {
        handleSnapshotInterference(rowId);

        return partitionStorage.addWrite(rowId, row, txId, commitTableOrZoneId, commitPartitionId);
    }

    @Override
    public AddWriteCommittedResult addWriteCommitted(
            RowId rowId,
            @Nullable BinaryRow row,
            HybridTimestamp commitTimestamp
    ) throws StorageException {
        handleSnapshotInterference(rowId);

        return partitionStorage.addWriteCommitted(rowId, row, commitTimestamp);
    }

    @Override
    public AbortResult abortWrite(RowId rowId, UUID txId) throws StorageException {
        handleSnapshotInterference(rowId);

        return partitionStorage.abortWrite(rowId, txId);
    }

    @Override
    public CommitResult commitWrite(RowId rowId, HybridTimestamp timestamp, UUID txId) throws StorageException {
        handleSnapshotInterference(rowId);

        return partitionStorage.commitWrite(rowId, timestamp, txId);
    }

    @Override
    public Cursor<ReadResult> scanVersions(RowId rowId) throws StorageException {
        return partitionStorage.scanVersions(rowId);
    }

    /**
     * Handles the situation when snapshots are running concurrently with write operations.
     * In case if a row that is going to be changed was not yet sent in an ongoing snapshot,
     * schedule an out-of-order sending of said row.
     *
     * @param rowId Row id.
     */
    private void handleSnapshotInterference(RowId rowId) {
        PartitionSnapshots partitionSnapshots = getPartitionSnapshots();

        for (OutgoingSnapshot snapshot : partitionSnapshots.ongoingSnapshots()) {
            snapshot.acquireMvLock();

            try {
                if (snapshot.alreadyPassedOrIrrelevant(tableId, rowId)) {
                    // Row already sent.
                    continue;
                }

                if (!snapshot.addRowIdToSkip(rowId)) {
                    // Already scheduled.
                    continue;
                }

                // Collect all versions of row and schedule the send operation.
                snapshot.enqueueForSending(tableId, rowId);
            } finally {
                snapshot.releaseMvLock();
            }
        }
    }

    @Override
    public void close() {
        if (partitionKey instanceof ZonePartitionKey) {
            // FIXME: This is a hack for the colocation feature, for zone-based partitions snapshots are cleaned up for a bunch of storages
            //  at once and this is done in a separate place. Should be removed as a part of
            //  https://issues.apache.org/jira/browse/IGNITE-22522
            return;
        }

        partitionsSnapshots.cleanupOutgoingSnapshots(partitionKey);
    }

    @Override
    @TestOnly
    public MvPartitionStorage getStorage() {
        return partitionStorage;
    }

    @Override
    public PartitionTimestampCursor scan(HybridTimestamp timestamp) throws StorageException {
        return partitionStorage.scan(timestamp);
    }

    @Override
    public @Nullable GcEntry peek(HybridTimestamp lowWatermark) {
        return partitionStorage.peek(lowWatermark);
    }

    @Override
    public @Nullable BinaryRow vacuum(GcEntry entry) {
        return partitionStorage.vacuum(entry);
    }

    @Override
    public void updateLease(LeaseInfo leaseInfo) {
        partitionStorage.updateLease(leaseInfo);
    }

    @Override
    public @Nullable LeaseInfo leaseInfo() {
        return partitionStorage.leaseInfo();
    }
}
