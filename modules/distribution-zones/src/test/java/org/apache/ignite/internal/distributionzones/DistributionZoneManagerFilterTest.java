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

package org.apache.ignite.internal.distributionzones;

import static java.util.UUID.randomUUID;
import static org.apache.ignite.internal.catalog.CatalogService.DEFAULT_STORAGE_PROFILE;
import static org.apache.ignite.internal.catalog.commands.CatalogUtils.IMMEDIATE_TIMER_VALUE;
import static org.apache.ignite.internal.distributionzones.DistributionZonesTestUtil.assertDataNodesFromManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.ignite.internal.catalog.descriptors.ConsistencyMode;
import org.apache.ignite.internal.cluster.management.topology.api.LogicalNode;
import org.apache.ignite.internal.network.ClusterNodeImpl;
import org.apache.ignite.network.NetworkAddress;
import org.junit.jupiter.api.Test;

/**
 * Tests distribution zone manager interactions with data nodes filtering in a SC zone.
 */
public class DistributionZoneManagerFilterTest extends BaseDistributionZoneManagerTest {
    private static final LogicalNode A = new LogicalNode(
            new ClusterNodeImpl(randomUUID(), "A", new NetworkAddress("localhost", 123)),
            Map.of("region", "US", "storage", "SSD", "dataRegionSize", "10"),
            Map.of(),
            List.of(DEFAULT_STORAGE_PROFILE)
    );

    private static final LogicalNode B = new LogicalNode(
            new ClusterNodeImpl(randomUUID(), "B", new NetworkAddress("localhost", 123)),
            Map.of("region", "EU", "storage", "HHD", "dataRegionSize", "30"),
            Map.of(),
            List.of(DEFAULT_STORAGE_PROFILE)
    );

    private static final LogicalNode C = new LogicalNode(
            new ClusterNodeImpl(randomUUID(), "C", new NetworkAddress("localhost", 123)),
            Map.of("region", "CN", "storage", "SSD", "dataRegionSize", "20"),
            Map.of(),
            List.of(DEFAULT_STORAGE_PROFILE)
    );

    private static final LogicalNode D = new LogicalNode(
            new ClusterNodeImpl(randomUUID(), "D", new NetworkAddress("localhost", 123)),
            Map.of("region", "CN", "storage", "SSD", "dataRegionSize", "20"),
            Map.of(),
            List.of(DEFAULT_STORAGE_PROFILE)
    );

    @Test
    void testFilterOnScaleUp() throws Exception {
        preparePrerequisites();

        topology.putNode(D);

        assertDataNodesFromManager(distributionZoneManager, metaStorageManager::appliedRevision, catalogManager::latestCatalogVersion,
                getZoneId(ZONE_NAME), Set.of(A, C, D), ZONE_MODIFICATION_AWAIT_TIMEOUT);
    }

    @Test
    void testFilterOnScaleDown() throws Exception {
        preparePrerequisites();

        topology.removeNodes(Set.of(C));

        assertDataNodesFromManager(distributionZoneManager, metaStorageManager::appliedRevision, catalogManager::latestCatalogVersion,
                getZoneId(ZONE_NAME), Set.of(A), ZONE_MODIFICATION_AWAIT_TIMEOUT);
    }

    @Test
    void testFilterOnScaleUpWithNewAttributesAfterRestart() throws Exception {
        preparePrerequisites();

        topology.removeNodes(Set.of(B));

        LogicalNode newB = new LogicalNode(
                new ClusterNodeImpl(randomUUID(), "newB", new NetworkAddress("localhost", 123)),
                Map.of("region", "US", "storage", "HHD", "dataRegionSize", "30"),
                Map.of(),
                List.of(DEFAULT_STORAGE_PROFILE)
        );

        topology.putNode(newB);

        assertDataNodesFromManager(distributionZoneManager, metaStorageManager::appliedRevision, catalogManager::latestCatalogVersion,
                getZoneId(ZONE_NAME), Set.of(A, newB, C), ZONE_MODIFICATION_AWAIT_TIMEOUT);
    }

    /**
     * Starts distribution zone manager with a zone and checks that two out of three nodes, which match filter,
     * are presented in the zones data nodes.
     *
     * @throws Exception If failed
     */
    private void preparePrerequisites() throws Exception {
        String filter = "$[?(@.storage == 'SSD' || @.region == 'US')]";

        startDistributionZoneManager();

        topology.putNode(A);
        topology.putNode(B);
        topology.putNode(C);

        createZone(ZONE_NAME, IMMEDIATE_TIMER_VALUE, IMMEDIATE_TIMER_VALUE, filter, consistencyMode(), DEFAULT_STORAGE_PROFILE);

        assertDataNodesFromManager(distributionZoneManager, metaStorageManager::appliedRevision, catalogManager::latestCatalogVersion,
                getZoneId(ZONE_NAME), Set.of(A, C), ZONE_MODIFICATION_AWAIT_TIMEOUT);
    }

    protected ConsistencyMode consistencyMode() {
        return ConsistencyMode.STRONG_CONSISTENCY;
    }
}
