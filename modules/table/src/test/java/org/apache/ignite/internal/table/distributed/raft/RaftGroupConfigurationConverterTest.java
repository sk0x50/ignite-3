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

package org.apache.ignite.internal.table.distributed.raft;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import org.apache.ignite.internal.raft.RaftGroupConfiguration;
import org.apache.ignite.internal.raft.RaftGroupConfigurationConverter;
import org.junit.jupiter.api.Test;

class RaftGroupConfigurationConverterTest {
    private final RaftGroupConfigurationConverter converter = new RaftGroupConfigurationConverter();

    @Test
    void convertsAndParses() {
        RaftGroupConfiguration configuration = new RaftGroupConfiguration(
                13L,
                37L,
                List.of("peer"),
                List.of("learner"),
                List.of("old-peer"),
                List.of("old-learner")
        );

        byte[] bytes = converter.toBytes(configuration);

        assertThat(converter.fromBytes(bytes), is(configuration));
    }

    @Test
    void parsesNullToNull() {
        assertThat(converter.fromBytes(null), is(nullValue()));
    }
}
