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

package org.apache.ignite.client.handler.requests.table;

import java.util.concurrent.CompletableFuture;
import org.apache.ignite.client.handler.ResponseWriter;
import org.apache.ignite.internal.table.TableViewInternal;
import org.apache.ignite.lang.util.IgniteNameUtils;
import org.apache.ignite.table.IgniteTables;

/**
 * Client tables retrieval request.
 */
public class ClientTablesGetRequest {
    /**
     * Processes the request.
     *
     * @param igniteTables Ignite tables.
     * @return Future.
     */
    public static CompletableFuture<ResponseWriter> process(
            IgniteTables igniteTables
    ) {
        return igniteTables.tablesAsync().thenApply(tables -> out -> {
            out.packInt(tables.size());

            for (var table : tables) {
                var tableImpl = (TableViewInternal) table;

                out.packInt(tableImpl.tableId());
                out.packString(quoteTableNameIfNotAllUpper(table.qualifiedName().objectName()));
            }
        });
    }

    private static String quoteTableNameIfNotAllUpper(String name) {
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);

            if (Character.isDigit(ch) || ch == '_') {
                continue;
            }

            if (!Character.isUpperCase(ch)) {
                return IgniteNameUtils.quoteIfNeeded(name);
            }
        }

        return name;
    }
}
