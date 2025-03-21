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

package org.apache.ignite.internal.catalog.descriptors;

import static org.apache.ignite.internal.catalog.storage.serialization.CatalogSerializationUtils.readArray;
import static org.apache.ignite.internal.catalog.storage.serialization.CatalogSerializationUtils.writeArray;

import java.io.IOException;
import org.apache.ignite.internal.catalog.descriptors.CatalogTableSchemaVersions.TableVersion;
import org.apache.ignite.internal.catalog.storage.serialization.CatalogEntrySerializerProvider;
import org.apache.ignite.internal.catalog.storage.serialization.CatalogObjectSerializer;
import org.apache.ignite.internal.catalog.storage.serialization.CatalogSerializer;
import org.apache.ignite.internal.catalog.storage.serialization.MarshallableEntryType;
import org.apache.ignite.internal.util.io.IgniteDataInput;
import org.apache.ignite.internal.util.io.IgniteDataOutput;

/**
 * Serializers for {@link CatalogTableSchemaVersions}.
 */
public class CatalogTableSchemaVersionsSerializers {
    /**
     * Serializer for {@link CatalogTableSchemaVersions}.
     */
    @CatalogSerializer(version = 1, since = "3.0.0")
    static class TableSchemaVersionsSerializerV1 implements CatalogObjectSerializer<CatalogTableSchemaVersions> {
        private final CatalogEntrySerializerProvider serializers;

        public TableSchemaVersionsSerializerV1(CatalogEntrySerializerProvider serializers) {
            this.serializers = serializers;
        }

        @Override
        public CatalogTableSchemaVersions readFrom(IgniteDataInput input) throws IOException {
            CatalogObjectSerializer<TableVersion> serializer =
                    serializers.get(1, MarshallableEntryType.DESCRIPTOR_TABLE_VERSION.id());

            TableVersion[] versions = readArray(serializer, input, TableVersion.class);
            int base = input.readVarIntAsInt();

            return new CatalogTableSchemaVersions(base, versions);
        }

        @Override
        public void writeTo(CatalogTableSchemaVersions tabVersions, IgniteDataOutput output) throws IOException {
            CatalogObjectSerializer<TableVersion> serializer =
                    serializers.get(1, MarshallableEntryType.DESCRIPTOR_TABLE_VERSION.id());

            writeArray(tabVersions.versions(), serializer, output);
            output.writeVarInt(tabVersions.earliestVersion());
        }
    }
}
