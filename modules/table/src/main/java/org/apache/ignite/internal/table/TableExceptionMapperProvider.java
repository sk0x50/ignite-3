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

package org.apache.ignite.internal.table;

import static org.apache.ignite.internal.lang.IgniteExceptionMapper.unchecked;

import com.google.auto.service.AutoService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.ignite.internal.lang.IgniteExceptionMapper;
import org.apache.ignite.internal.lang.IgniteExceptionMappersProvider;
import org.apache.ignite.internal.partition.replicator.schemacompat.IncompatibleSchemaVersionException;
import org.apache.ignite.tx.IncompatibleSchemaException;

/**
 * Table module exception mapper.
 */
@AutoService(IgniteExceptionMappersProvider.class)
public class TableExceptionMapperProvider implements IgniteExceptionMappersProvider {
    @Override
    public Collection<IgniteExceptionMapper<?, ?>> mappers() {
        List<IgniteExceptionMapper<?, ?>> mappers = new ArrayList<>();

        mappers.add(unchecked(
                IncompatibleSchemaVersionException.class,
                err -> new IncompatibleSchemaException(err.traceId(), err.code(), err.getMessage(), err)
        ));

        return mappers;
    }
}
