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

import static org.apache.ignite.internal.catalog.commands.CatalogUtils.defaultZoneIdOpt;
import static org.apache.ignite.internal.catalog.commands.CatalogUtils.indexOrThrow;
import static org.apache.ignite.internal.catalog.commands.CatalogUtils.replaceIndex;
import static org.apache.ignite.internal.catalog.commands.CatalogUtils.replaceSchema;
import static org.apache.ignite.internal.catalog.commands.CatalogUtils.schemaOrThrow;
import static org.apache.ignite.internal.catalog.commands.CatalogUtils.tableOrThrow;

import org.apache.ignite.internal.catalog.Catalog;
import org.apache.ignite.internal.catalog.CatalogValidationException;
import org.apache.ignite.internal.catalog.descriptors.CatalogHashIndexDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogIndexDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogSchemaDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogSortedIndexDescriptor;
import org.apache.ignite.internal.catalog.descriptors.CatalogTableDescriptor;
import org.apache.ignite.internal.catalog.storage.serialization.MarshallableEntryType;
import org.apache.ignite.internal.hlc.HybridTimestamp;
import org.apache.ignite.internal.tostring.S;

/** Entry representing a rename of an index. */
public class RenameIndexEntry implements UpdateEntry {
    private final int indexId;

    private final String newIndexName;

    public RenameIndexEntry(int indexId, String newIndexName) {
        this.indexId = indexId;
        this.newIndexName = newIndexName;
    }

    public int indexId() {
        return indexId;
    }

    public String newIndexName() {
        return newIndexName;
    }

    @Override
    public int typeId() {
        return MarshallableEntryType.RENAME_INDEX.id();
    }

    @Override
    public Catalog applyUpdate(Catalog catalog, HybridTimestamp timestamp) {
        CatalogIndexDescriptor indexDescriptor = indexOrThrow(catalog, indexId);

        CatalogTableDescriptor tableDescriptor = tableOrThrow(catalog, indexDescriptor.tableId());

        CatalogSchemaDescriptor schemaDescriptor = schemaOrThrow(catalog, tableDescriptor.schemaId());

        CatalogIndexDescriptor newIndexDescriptor = changeIndexName(indexDescriptor, timestamp);

        return new Catalog(
                catalog.version(),
                catalog.time(),
                catalog.objectIdGenState(),
                catalog.zones(),
                replaceSchema(replaceIndex(schemaDescriptor, newIndexDescriptor), catalog.schemas()),
                defaultZoneIdOpt(catalog)
        );
    }

    private CatalogIndexDescriptor changeIndexName(CatalogIndexDescriptor indexDescriptor, HybridTimestamp timestamp) {
        CatalogIndexDescriptor newIndexDescriptor;

        if (indexDescriptor instanceof CatalogHashIndexDescriptor) {
            newIndexDescriptor = changeHashIndexName((CatalogHashIndexDescriptor) indexDescriptor);
        } else if (indexDescriptor instanceof CatalogSortedIndexDescriptor) {
            newIndexDescriptor = changeSortedIndexName((CatalogSortedIndexDescriptor) indexDescriptor);
        } else {
            throw new CatalogValidationException("Unsupported index type '{}' {}", indexDescriptor.id(), indexDescriptor);
        }

        newIndexDescriptor.updateTimestamp(timestamp);

        return newIndexDescriptor;
    }

    private CatalogIndexDescriptor changeHashIndexName(CatalogHashIndexDescriptor index) {
        return new CatalogHashIndexDescriptor(
                index.id(),
                newIndexName,
                index.tableId(),
                index.unique(),
                index.status(),
                index.columns(),
                index.isCreatedWithTable()
        );
    }

    private CatalogIndexDescriptor changeSortedIndexName(CatalogSortedIndexDescriptor index) {
        return new CatalogSortedIndexDescriptor(
                index.id(),
                newIndexName,
                index.tableId(),
                index.unique(),
                index.status(),
                index.columns(),
                index.isCreatedWithTable()
        );
    }

    @Override
    public String toString() {
        return S.toString(this);
    }
}
