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

package org.apache.ignite.internal.sql.engine.exec.exp;

import static org.apache.ignite.internal.sql.engine.exec.exp.ExpressionFactoryImpl.digest;
import static org.apache.ignite.internal.sql.engine.util.Commons.cast;

import java.lang.reflect.Modifier;
import java.util.List;
import org.apache.calcite.DataContext;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.linq4j.function.Function1;
import org.apache.calcite.linq4j.tree.BlockBuilder;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.calcite.linq4j.tree.MethodDeclaration;
import org.apache.calcite.linq4j.tree.ParameterExpression;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexProgram;
import org.apache.calcite.rex.RexProgramBuilder;
import org.apache.calcite.sql.validate.SqlConformance;
import org.apache.ignite.internal.sql.engine.exec.ExecutionContext;
import org.apache.ignite.internal.sql.engine.exec.exp.RexToLixTranslator.InputGetter;
import org.apache.ignite.internal.sql.engine.util.Commons;
import org.apache.ignite.internal.sql.engine.util.IgniteMethod;
import org.apache.ignite.internal.sql.engine.util.cache.Cache;
import org.apache.ignite.lang.ErrorGroups.Sql;
import org.apache.ignite.sql.SqlException;

/** Implementor which implements {@link SqlJoinPredicate}. */
class JoinPredicateImplementor {
    private final Cache<String, Object> cache;
    private final RexBuilder rexBuilder;
    private final JavaTypeFactory typeFactory;
    private final SqlConformance conformance;

    JoinPredicateImplementor(
            Cache<String, Object> cache,
            RexBuilder rexBuilder,
            JavaTypeFactory typeFactory,
            SqlConformance conformance
    ) {
        this.cache = cache;
        this.rexBuilder = rexBuilder;
        this.typeFactory = typeFactory;
        this.conformance = conformance;
    }

    /**
     * Implements given expression as {@link SqlJoinPredicate}.
     *
     * @param predicateExpression The expression to implement.
     * @param type The type of the input row as if rows from both sides will be joined.
     * @param firstRowSize Size of the first (left) row. Used to adjust index and route request to a proper row.
     * @param <RowT> The type of the execution row.
     * @return An implementation of join predicate.
     * @see SqlJoinPredicate
     */
    <RowT> SqlJoinPredicate<RowT> implement(RexNode predicateExpression, RelDataType type, int firstRowSize) {
        String digest = digest(SqlJoinPredicate.class, List.of(predicateExpression), type, "firstRowSize=" + firstRowSize);
        Cache<String, SqlJoinPredicate<RowT>> cache = cast(this.cache);

        return cache.get(digest, ignored -> implementInternal(predicateExpression, type, firstRowSize));
    }

    private <RowT> SqlJoinPredicate<RowT> implementInternal(RexNode predicateExpression, RelDataType type, int firstRowSize) {
        RexProgramBuilder programBuilder = new RexProgramBuilder(type, rexBuilder);

        programBuilder.addCondition(predicateExpression);

        RexProgram program = programBuilder.getProgram();

        BlockBuilder builder = new BlockBuilder();

        ParameterExpression ctx = Expressions.parameter(ExecutionContext.class, "ctx");
        ParameterExpression left = Expressions.parameter(Object.class, "left");
        ParameterExpression right = Expressions.parameter(Object.class, "right");

        builder.add(
                Expressions.declare(Modifier.FINAL, DataContext.ROOT, Expressions.convert_(ctx, DataContext.class))
        );

        Expression rowHandler = builder.append("hnd", Expressions.call(ctx, IgniteMethod.CONTEXT_ROW_HANDLER.method()));

        InputGetter inputGetter = new BiFieldGetter(rowHandler, left, right, type, firstRowSize);

        Function1<String, InputGetter> correlates = new CorrelatesBuilder(builder, ctx, rowHandler)
                .build(List.of(predicateExpression));

        Expression condition = RexToLixTranslator.translateCondition(program, typeFactory, builder,
                inputGetter, correlates, conformance, ctx);

        builder.add(condition);

        ParameterExpression ex = Expressions.parameter(0, Exception.class, "e");
        Expression sqlException = Expressions.new_(SqlException.class, Expressions.constant(Sql.RUNTIME_ERR), ex);
        BlockBuilder tryCatchBlock = new BlockBuilder();

        tryCatchBlock.add(Expressions.tryCatch(builder.toBlock(), Expressions.catch_(ex, Expressions.throw_(sqlException))));

        List<ParameterExpression> params = List.of(ctx, left, right);

        MethodDeclaration declaration = Expressions.methodDecl(
                Modifier.PUBLIC, boolean.class, "test",
                params, tryCatchBlock.toBlock());

        Class<SqlJoinPredicate<RowT>> clazz = cast(SqlJoinPredicate.class);

        String body = Expressions.toString(List.of(declaration), "\n", false);

        return Commons.compile(clazz, body);
    }
}
