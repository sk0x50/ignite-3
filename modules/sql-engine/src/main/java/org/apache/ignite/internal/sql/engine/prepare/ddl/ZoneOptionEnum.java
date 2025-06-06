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

package org.apache.ignite.internal.sql.engine.prepare.ddl;

/**
 * Enumerates the options for CREATE ZONE and ALTER ZONE statements.
 */
public enum ZoneOptionEnum {

    /** Number of partitions. */
    PARTITIONS("PARTITIONS"),

    /** Number of replicas. */
    REPLICAS("REPLICAS"),

    /** Quorum size. */
    QUORUM_SIZE("QUORUM SIZE"),

    /** Partition distribution algorithm name. */
    DISTRIBUTION_ALGORITHM("DISTRIBUTION ALGORITHM"),

    /** An expression to filter data nodes. */
    DATA_NODES_FILTER("NODES FILTER"),

    /** Data nodes auto adjust timeout. */
    DATA_NODES_AUTO_ADJUST("AUTO ADJUST"),

    /** Data nodes scale up auto adjust timeout. */
    DATA_NODES_AUTO_ADJUST_SCALE_UP("AUTO SCALE UP"),

    /** Data nodes scale down auto adjust timeout. */
    DATA_NODES_AUTO_ADJUST_SCALE_DOWN("AUTO SCALE DOWN"),

    /** Consistency mode. */
    CONSISTENCY_MODE("CONSISTENCY MODE");

    /** Name of the property as it appears in sql grammar. That is, this name may be used to build valid sql string. */
    public final String sqlName;

    ZoneOptionEnum(String sqlName) {
        this.sqlName = sqlName;
    }
}
