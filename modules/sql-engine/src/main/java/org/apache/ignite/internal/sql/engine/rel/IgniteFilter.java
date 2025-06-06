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

package org.apache.ignite.internal.sql.engine.rel;

import static org.apache.ignite.internal.sql.engine.trait.TraitUtils.changeTraits;

import java.util.List;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptCost;
import org.apache.calcite.plan.RelOptPlanner;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelInput;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Filter;
import org.apache.calcite.rel.metadata.RelMetadataQuery;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.util.Pair;
import org.apache.ignite.internal.sql.engine.metadata.cost.IgniteCost;
import org.apache.ignite.internal.sql.engine.rel.explain.IgniteRelWriter;
import org.apache.ignite.internal.sql.engine.trait.TraitUtils;
import org.apache.ignite.internal.sql.engine.trait.TraitsAwareIgniteRel;

/**
 * Relational expression that iterates over its input and returns elements for which <code>condition</code> evaluates to
 * <code>true</code>.
 *
 * <p>If the condition allows nulls, then a null value is treated the same as
 * false.</p>
 */
public class IgniteFilter extends Filter implements TraitsAwareIgniteRel {
    private static final String REL_TYPE_NAME = "Filter";

    /**
     * Creates a filter.
     *
     * @param cluster   Cluster that this relational expression belongs to.
     * @param traits    The traits of this rel.
     * @param input     Input relational expression.
     * @param condition boolean expression which determines whether a row is allowed to pass.
     */
    public IgniteFilter(RelOptCluster cluster, RelTraitSet traits, RelNode input, RexNode condition) {
        super(cluster, traits, input, condition);
    }

    /**
     * Constructor used for deserialization.
     *
     * @param input Serialized representation.
     */
    public IgniteFilter(RelInput input) {
        super(changeTraits(input, IgniteConvention.INSTANCE));
    }

    /** {@inheritDoc} */
    @Override
    public Filter copy(RelTraitSet traitSet, RelNode input, RexNode condition) {
        return new IgniteFilter(getCluster(), traitSet, input, condition);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T accept(IgniteRelVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /** {@inheritDoc} */
    @Override
    public List<Pair<RelTraitSet, List<RelTraitSet>>> deriveDistribution(RelTraitSet nodeTraits,
            List<RelTraitSet> inTraits) {
        return List.of(Pair.of(nodeTraits.replace(TraitUtils.distribution(inTraits.get(0))),
                inTraits));
    }

    /** {@inheritDoc} */
    @Override
    public List<Pair<RelTraitSet, List<RelTraitSet>>> deriveCollation(RelTraitSet nodeTraits,
            List<RelTraitSet> inTraits) {
        return List.of(Pair.of(nodeTraits.replace(TraitUtils.collation(inTraits.get(0))),
                inTraits));
    }

    /** {@inheritDoc} */
    @Override
    public RelOptCost computeSelfCost(RelOptPlanner planner, RelMetadataQuery mq) {
        double rowCount = mq.getRowCount(getInput());

        return planner.getCostFactory().makeCost(rowCount,
                rowCount * (IgniteCost.ROW_COMPARISON_COST + IgniteCost.ROW_PASS_THROUGH_COST), 0);
    }

    /** {@inheritDoc} */
    @Override
    public IgniteRel clone(RelOptCluster cluster, List<IgniteRel> inputs) {
        return new IgniteFilter(cluster, getTraitSet(), sole(inputs), getCondition());
    }

    /** {@inheritDoc} */
    @Override public String getRelTypeName() {
        return REL_TYPE_NAME;
    }

    @Override
    public IgniteRelWriter explain(IgniteRelWriter writer) {
        return writer.addPredicate(condition, getInput().getRowType());
    }
}
