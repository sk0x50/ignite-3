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

package org.apache.ignite.internal.storage.rocksdb.index;

import static org.apache.ignite.internal.catalog.commands.CatalogUtils.DEFAULT_PARTITION_COUNT;
import static org.apache.ignite.internal.util.IgniteUtils.closeAll;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.ignite.internal.components.LogSyncer;
import org.apache.ignite.internal.configuration.testframework.ConfigurationExtension;
import org.apache.ignite.internal.configuration.testframework.InjectConfiguration;
import org.apache.ignite.internal.failure.FailureProcessor;
import org.apache.ignite.internal.storage.configurations.StorageConfiguration;
import org.apache.ignite.internal.storage.engine.StorageTableDescriptor;
import org.apache.ignite.internal.storage.index.AbstractHashIndexStorageTest;
import org.apache.ignite.internal.storage.index.StorageIndexDescriptorSupplier;
import org.apache.ignite.internal.storage.rocksdb.RocksDbStorageEngine;
import org.apache.ignite.internal.testframework.ExecutorServiceExtension;
import org.apache.ignite.internal.testframework.InjectExecutorService;
import org.apache.ignite.internal.testframework.WorkDirectory;
import org.apache.ignite.internal.testframework.WorkDirectoryExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests for the {@link RocksDbHashIndexStorage} class.
 */
@ExtendWith(ExecutorServiceExtension.class)
@ExtendWith(WorkDirectoryExtension.class)
@ExtendWith(ConfigurationExtension.class)
public class RocksDbHashIndexStorageTest extends AbstractHashIndexStorageTest {
    private RocksDbStorageEngine engine;

    @BeforeEach
    void setUp(
            @WorkDirectory Path workDir,
            // Explicit size, small enough for fast allocation, and big enough to fit some data without flushing it to disk constantly.
            @InjectConfiguration("mock.profiles.default = {engine = rocksdb, sizeBytes = 16777216, writeBufferSizeBytes = 67108864}")
            StorageConfiguration storageConfiguration,
            @InjectExecutorService
            ScheduledExecutorService scheduledExecutor
    ) {
        engine = new RocksDbStorageEngine(
                "test",
                storageConfiguration,
                workDir,
                mock(LogSyncer.class),
                scheduledExecutor,
                mock(FailureProcessor.class)
        );

        engine.start();

        tableStorage = engine.createMvTable(
                new StorageTableDescriptor(1, DEFAULT_PARTITION_COUNT, "default"),
                mock(StorageIndexDescriptorSupplier.class)
        );

        initialize(tableStorage);
    }

    @AfterEach
    void tearDown() throws Exception {
        closeAll(
                tableStorage == null ? null : tableStorage::close,
                engine == null ? null : engine::stop
        );
    }
}
