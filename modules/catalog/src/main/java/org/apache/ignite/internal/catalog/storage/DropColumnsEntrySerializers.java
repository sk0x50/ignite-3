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

import static org.apache.ignite.internal.catalog.storage.serialization.CatalogSerializationUtils.writeStringCollection;
import static org.apache.ignite.internal.util.IgniteUtils.capacity;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.apache.ignite.internal.catalog.storage.serialization.CatalogObjectSerializer;
import org.apache.ignite.internal.catalog.storage.serialization.CatalogSerializationUtils;
import org.apache.ignite.internal.catalog.storage.serialization.CatalogSerializer;
import org.apache.ignite.internal.util.io.IgniteDataInput;
import org.apache.ignite.internal.util.io.IgniteDataOutput;

/**
 * Serializers for {@link DropColumnsEntry}.
 */
public class DropColumnsEntrySerializers {
    /**
     * Serializer for {@link DropColumnsEntry}.
     */
    @CatalogSerializer(version = 1, since = "3.0.0")
    static class DropColumnEntrySerializerV1 implements CatalogObjectSerializer<DropColumnsEntry> {
        @Override
        public DropColumnsEntry readFrom(IgniteDataInput input) throws IOException {
            int tableId = input.readVarIntAsInt();
            Set<String> columns = CatalogSerializationUtils.readStringCollection(input, size -> new HashSet<>(capacity(size)));

            return new DropColumnsEntry(tableId, columns);
        }

        @Override
        public void writeTo(DropColumnsEntry object, IgniteDataOutput output) throws IOException {
            output.writeVarInt(object.tableId());
            writeStringCollection(object.columns(), output);
        }
    }
}
