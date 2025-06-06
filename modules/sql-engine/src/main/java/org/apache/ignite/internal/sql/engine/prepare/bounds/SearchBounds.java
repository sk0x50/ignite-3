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

package org.apache.ignite.internal.sql.engine.prepare.bounds;

import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.jetbrains.annotations.Nullable;

/**
 * Bounds holder for search row.
 *
 * <p>This class is describing bounds on per-column basis, and such choice was made to optimize serialization of a plan.
 */
public abstract class SearchBounds {
    /** Condition required only for cost calculation, no serialization needed. */
    private final RexNode condition;

    /**
     * Creates search bounds.
     *
     * @param condition Condition.
     */
    protected SearchBounds(@Nullable RexNode condition) {
        this.condition = condition;
    }

    /**
     * Condition.
     */
    public @Nullable RexNode condition() {
        return condition;
    }

    /**
     * Returns bounds type.
     */
    public abstract Type type();

    /**
     * Applies this shuttle and returns a new SearchBounds instance if changed.
     */
    public abstract SearchBounds accept(RexShuttle shuttle);

    /**
     * Search bounds type.
     */
    public enum Type {
        /** Exact search value. */
        EXACT,

        /** Range of values. */
        RANGE,

        /** Multiple values or multiple ranges. */
        MULTI
    }
}
