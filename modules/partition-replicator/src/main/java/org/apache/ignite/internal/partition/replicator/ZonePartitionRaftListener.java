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

package org.apache.ignite.internal.partition.replicator;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.function.Consumer;
import org.apache.ignite.internal.raft.ReadCommand;
import org.apache.ignite.internal.raft.WriteCommand;
import org.apache.ignite.internal.raft.service.CommandClosure;
import org.apache.ignite.internal.raft.service.RaftGroupListener;

/**
 * RAFT listener for the zone partition.
 */
public class ZonePartitionRaftListener implements RaftGroupListener {

    @Override
    public void onRead(Iterator<CommandClosure<ReadCommand>> iterator) {
        // No-op
    }

    @Override
    public void onWrite(Iterator<CommandClosure<WriteCommand>> iterator) {
        // No-op
    }

    @Override
    public void onSnapshotSave(Path path, Consumer<Throwable> doneClo) {
        throw new UnsupportedOperationException("Snapshotting is not implemented");
    }

    @Override
    public boolean onSnapshotLoad(Path path) {
        throw new UnsupportedOperationException("Snapshotting is not implemented");
    }

    @Override
    public void onShutdown() {
        // No-op
    }
}
