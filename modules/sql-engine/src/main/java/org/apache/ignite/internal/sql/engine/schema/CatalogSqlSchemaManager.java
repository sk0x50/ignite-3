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

package org.apache.ignite.internal.sql.engine.schema;

import static org.apache.ignite.internal.catalog.CatalogService.DEFAULT_SCHEMA_NAME;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.tools.Frameworks;
import org.apache.ignite.internal.catalog.CatalogManager;
import org.apache.ignite.internal.catalog.CatalogService;
import org.apache.ignite.internal.catalog.commands.DefaultValue;
import org.apache.ignite.internal.catalog.commands.DefaultValue.ConstantValue;
import org.apache.ignite.internal.catalog.commands.DefaultValue.FunctionCall;
import org.apache.ignite.internal.catalog.descriptors.CatalogHashIndexDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogIndexDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogSchemaDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogSortedIndexDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogTableColumnDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogTableDescriptor;
import org.apache.ignite.internal.schema.DefaultValueGenerator;
import org.apache.ignite.internal.sql.engine.schema.IgniteIndex.Type;
import org.apache.ignite.internal.sql.engine.trait.IgniteDistribution;
import org.apache.ignite.internal.sql.engine.trait.IgniteDistributions;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link SqlSchemaManager} backed by {@link CatalogService}.
 */
public class CatalogSqlSchemaManager implements SqlSchemaManager {

    private final CatalogManager catalogManager;

    private final ConcurrentMap<Map.Entry<String, Integer>, SchemaPlus> cache;

    /** Constructor. */
    public CatalogSqlSchemaManager(CatalogManager catalogManager, int cacheSize) {
        this.catalogManager = catalogManager;
        this.cache = Caffeine.newBuilder().maximumSize(cacheSize).<Map.Entry<String, Integer>, SchemaPlus>build().asMap();
    }

    /** {@inheritDoc} */
    @Override
    public SchemaPlus schema(@Nullable String schema) {
        // Should be removed -schema(name, version) must be used instead
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public SchemaPlus schema(String name, int version) {
        String schemaName = name == null ? DEFAULT_SCHEMA_NAME : name;

        Entry<String, Integer> entry = Map.entry(schemaName, version);
        return cache.computeIfAbsent(entry, (e) -> createSqlSchema(e.getValue(), catalogManager.schema(e.getKey(), e.getValue())));
    }

    /** {@inheritDoc} */
    @Override
    public IgniteTable tableById(int id) {
        // Should be removed - this method is used to obtain native types from a table.
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<?> actualSchemaAsync(long ver) {
        return CompletableFuture.completedFuture(catalogManager.schema((int) ver));
    }

    /** {@inheritDoc} */
    @Override
    public SchemaPlus activeSchema(@Nullable String name, long timestamp) {
        String schemaName = name == null ? DEFAULT_SCHEMA_NAME : name;

        int version = catalogManager.activeCatalogVersion(timestamp);

        CatalogSchemaDescriptor descriptor = catalogManager.schema(schemaName, version);

        Entry<String, Integer> entry = Map.entry(schemaName, version);
        return cache.computeIfAbsent(entry, (v) -> createSqlSchema(v.getValue(), descriptor));
    }

    private SchemaPlus createSqlSchema(int version, CatalogSchemaDescriptor descriptor) {
        String schemaName = descriptor.name();

        int numTables = descriptor.tables().length;
        Map<String, Table> schemaTables = new HashMap<>(numTables);
        Map<Integer, TableDescriptorImpl> tableDescriptorMap = new LinkedHashMap<>(numTables);

        // Assemble sql-engine.TableDescriptors as they are required by indexes.
        for (CatalogTableDescriptor tableDescriptor : descriptor.tables()) {
            TableDescriptorImpl descriptorImpl = createTableDescriptor(tableDescriptor);
            tableDescriptorMap.put(tableDescriptor.id(), descriptorImpl);
        }

        Map<Integer, Map<String, IgniteSchemaIndex>> schemaTableIndexes = new HashMap<>(descriptor.indexes().length);

        // Assemble indexes as they are required by tables.
        for (CatalogIndexDescriptor indexDescriptor : descriptor.indexes()) {
            int tableId = indexDescriptor.tableId();
            TableDescriptorImpl tableDescriptorImpl = tableDescriptorMap.get(tableId);
            assert tableDescriptorImpl != null : "Table is not found in schema: " + tableId;

            String indexName = indexDescriptor.name();
            Map<String, IgniteSchemaIndex> tableIndexes = schemaTableIndexes.computeIfAbsent(tableId, id -> new LinkedHashMap<>());

            Type type;
            if (indexDescriptor instanceof CatalogSortedIndexDescriptor) {
                type = Type.SORTED;
            } else if (indexDescriptor instanceof CatalogHashIndexDescriptor) {
                type = Type.HASH;
            } else {
                throw new IllegalArgumentException("Unexpected index type: " + indexDescriptor);
            }

            RelCollation indexCollation = IgniteSchemaIndex.createIndexCollation(indexDescriptor, tableDescriptorImpl);
            IgniteSchemaIndex schemaIndex = new IgniteSchemaIndex(indexName, type, tableDescriptorImpl.distribution(), indexCollation);
            tableIndexes.put(indexName, schemaIndex);

            schemaTableIndexes.put(tableId, tableIndexes);
        }

        // Assemble tables.
        for (CatalogTableDescriptor tableDescriptor : descriptor.tables()) {
            int tableId = tableDescriptor.id();
            String tableName = tableDescriptor.name();
            TableDescriptorImpl descriptorImpl = tableDescriptorMap.get(tableId);
            assert descriptorImpl != null;

            IgniteStatistic statistic = new IgniteStatistic(() -> 0.0d, descriptorImpl.distribution());
            Map<String, IgniteSchemaIndex> tableIndexMap = schemaTableIndexes.getOrDefault(tableId, Collections.emptyMap());

            IgniteSchemaTable schemaTable = new IgniteSchemaTable(tableName, tableId, version, descriptorImpl, statistic, tableIndexMap);

            schemaTables.put(tableName, schemaTable);
        }

        // create root schema
        SchemaPlus rootSchema = Frameworks.createRootSchema(false);
        IgniteCatalogSchema igniteSchema = new IgniteCatalogSchema(schemaName, version, schemaTables);
        return rootSchema.add(schemaName, igniteSchema);
    }

    private static TableDescriptorImpl createTableDescriptor(CatalogTableDescriptor descriptor) {
        List<ColumnDescriptor> colDescriptors = new ArrayList<>();
        List<Integer> colocationColumns = new ArrayList<>();

        List<CatalogTableColumnDescriptor> columns = descriptor.columns();
        for (int i = 0; i < columns.size(); i++) {
            CatalogTableColumnDescriptor col = columns.get(i);
            boolean nullable = col.nullable();
            boolean key = descriptor.isPrimaryKeyColumn(col.name());

            DefaultValue defaultVal = col.defaultValue();
            DefaultValueStrategy defaultValueStrategy;
            Supplier<Object> defaultValueSupplier;

            if (defaultVal != null) {
                switch (defaultVal.type()) {
                    case CONSTANT:
                        ConstantValue constantValue = (ConstantValue) defaultVal;
                        if (constantValue.value() == null) {
                            defaultValueStrategy = DefaultValueStrategy.DEFAULT_NULL;
                            defaultValueSupplier = () -> null;
                        } else {
                            defaultValueStrategy = DefaultValueStrategy.DEFAULT_CONSTANT;
                            defaultValueSupplier = constantValue::value;
                        }
                        break;
                    case FUNCTION_CALL:
                        FunctionCall functionCall = (FunctionCall) defaultVal;
                        String functionName = functionCall.functionName().toUpperCase(Locale.US);
                        DefaultValueGenerator defaultValueGenerator = DefaultValueGenerator.valueOf(functionName);
                        defaultValueStrategy = DefaultValueStrategy.DEFAULT_COMPUTED;
                        defaultValueSupplier = defaultValueGenerator::next;
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected default value: ");
                }
            } else {
                defaultValueStrategy = null;
                defaultValueSupplier = null;
            }


            CatalogColumnDescriptor columnDescriptor = new CatalogColumnDescriptor(
                    col.name(),
                    key,
                    nullable,
                    i,
                    col.type(),
                    col.precision(),
                    col.scale(),
                    defaultValueStrategy,
                    defaultValueSupplier
            );

            if (descriptor.isColocationColumn(col.name())) {
                colocationColumns.add(i);
            }

            colDescriptors.add(columnDescriptor);
        }

        // TODO Use the actual zone ID after implementing https://issues.apache.org/jira/browse/IGNITE-18426.
        int tableId = descriptor.id();
        IgniteDistribution distribution = IgniteDistributions.affinity(colocationColumns, tableId, tableId);

        return new TableDescriptorImpl(colDescriptors, distribution);
    }
}