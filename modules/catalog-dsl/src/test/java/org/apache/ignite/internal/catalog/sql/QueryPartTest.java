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

package org.apache.ignite.internal.catalog.sql;

import static org.apache.ignite.catalog.ColumnSorted.column;
import static org.apache.ignite.catalog.ColumnType.BIGINT;
import static org.apache.ignite.catalog.ColumnType.BOOLEAN;
import static org.apache.ignite.catalog.ColumnType.DATE;
import static org.apache.ignite.catalog.ColumnType.DECIMAL;
import static org.apache.ignite.catalog.ColumnType.DOUBLE;
import static org.apache.ignite.catalog.ColumnType.FLOAT;
import static org.apache.ignite.catalog.ColumnType.INT16;
import static org.apache.ignite.catalog.ColumnType.INT32;
import static org.apache.ignite.catalog.ColumnType.INT64;
import static org.apache.ignite.catalog.ColumnType.INT8;
import static org.apache.ignite.catalog.ColumnType.INTEGER;
import static org.apache.ignite.catalog.ColumnType.REAL;
import static org.apache.ignite.catalog.ColumnType.SMALLINT;
import static org.apache.ignite.catalog.ColumnType.TIME;
import static org.apache.ignite.catalog.ColumnType.TIMESTAMP;
import static org.apache.ignite.catalog.ColumnType.TINYINT;
import static org.apache.ignite.catalog.ColumnType.UUID;
import static org.apache.ignite.catalog.ColumnType.VARBINARY;
import static org.apache.ignite.catalog.ColumnType.VARCHAR;
import static org.apache.ignite.internal.catalog.sql.ColumnTypeImpl.wrap;
import static org.apache.ignite.internal.catalog.sql.IndexColumnImpl.parseColumn;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ignite.catalog.ColumnType;
import org.apache.ignite.catalog.IndexType;
import org.apache.ignite.catalog.SortOrder;
import org.junit.jupiter.api.Test;

class QueryPartTest {
    @Test
    void namePart() {
        Name name = new Name("a");
        assertThat(sql(name), is("a"));

        name = new Name("a", "b", "c");
        assertThat(sql(name), is("a.b.c"));
    }

    @Test
    void colocatePart() {
        Colocate colocate = new Colocate("a");
        assertThat(sql(colocate), is("COLOCATE BY (a)"));

        colocate = new Colocate("a", "b");
        assertThat(sql(colocate), is("COLOCATE BY (a, b)"));
    }

    @Test
    void columnPart() {
        Column column = new Column("a", wrap(VARCHAR));
        assertThat(sql(column), is("a varchar"));

        column = new Column("a", wrap(ColumnType.varchar(3)));
        assertThat(sql(column), is("a varchar(3)"));

        column = new Column("a", wrap(ColumnType.decimal(2, 3)));
        assertThat(sql(column), is("a decimal(2, 3)"));
    }

    @Test
    void columnTypePart() {
        assertThat(sql(wrap(BOOLEAN)), is("boolean"));
        assertThat(sql(wrap(TINYINT)), is("tinyint"));
        assertThat(sql(wrap(SMALLINT)), is("smallint"));
        assertThat(sql(wrap(INT8)), is("tinyint"));
        assertThat(sql(wrap(INT16)), is("smallint"));
        assertThat(sql(wrap(INT32)), is("int"));
        assertThat(sql(wrap(INT64)), is("bigint"));
        assertThat(sql(wrap(INTEGER)), is("int"));
        assertThat(sql(wrap(BIGINT)), is("bigint"));
        assertThat(sql(wrap(REAL)), is("real"));
        assertThat(sql(wrap(FLOAT)), is("real"));
        assertThat(sql(wrap(DOUBLE)), is("double"));
        assertThat(sql(wrap(VARCHAR)), is("varchar"));
        assertThat(sql(wrap(ColumnType.varchar(1))), is("varchar(1)"));
        assertThat(sql(wrap(VARBINARY)), is("varbinary"));
        assertThat(sql(wrap(ColumnType.varbinary(1))), is("varbinary(1)"));
        assertThat(sql(wrap(TIME)), is("time"));
        assertThat(sql(wrap(ColumnType.time(1))), is("time(1)"));
        assertThat(sql(wrap(TIMESTAMP)), is("timestamp"));
        assertThat(sql(wrap(ColumnType.timestamp(1))), is("timestamp(1)"));
        assertThat(sql(wrap(DATE)), is("date"));
        assertThat(sql(wrap(DECIMAL)), is("decimal"));
        assertThat(sql(wrap(ColumnType.decimal(1, 2))), is("decimal(1, 2)"));
        assertThat(sql(wrap(UUID)), is("uuid"));
    }

    @Test
    void columnTypeOptionsPart() {
        assertThat(sql(wrap(INTEGER)), is("int"));
        assertThat(sql(wrap(INTEGER.notNull())), is("int NOT NULL"));
        assertThat(sql(wrap(INTEGER.defaultValue(1))), is("int DEFAULT 1"));
        assertThat(sql(wrap(VARCHAR.defaultValue("s"))), is("varchar DEFAULT 's'")); // default in single quotes
        assertThat(sql(wrap(INTEGER.defaultExpression("gen_expr"))), is("int DEFAULT gen_expr"));
        assertThat(sql(wrap(INTEGER.notNull().defaultValue(1))), is("int NOT NULL DEFAULT 1"));
        assertThat(sql(wrap(ColumnType.decimal(2, 3).defaultValue(BigDecimal.ONE).notNull())), is("decimal(2, 3) NOT NULL DEFAULT 1"));
        assertThat(sql(wrap(INTEGER.defaultValue(1).defaultExpression("gen_expr"))), is("int DEFAULT 1"));
    }

    @Test
    void constraintPart() {
        Constraint constraint = new Constraint().primaryKey(column("a"));
        assertThat(sql(constraint), is("PRIMARY KEY (a)"));

        constraint = new Constraint().primaryKey(IndexType.SORTED, List.of(column("a")));
        assertThat(sql(constraint), is("PRIMARY KEY USING SORTED (a)"));

        constraint = new Constraint().primaryKey(column("a"), column("b"));
        assertThat(sql(constraint), is("PRIMARY KEY (a, b)"));

        constraint = new Constraint().primaryKey(IndexType.SORTED, List.of(column("a"), column("b")));
        assertThat(sql(constraint), is("PRIMARY KEY USING SORTED (a, b)"));
    }

    @Test
    void queryPartCollection() {
        QueryPartCollection<Name> collection = QueryPartCollection.partsList(new Name("a"), new Name("b"));

        assertThat(sql(collection), is("a, b"));
    }

    @Test
    void indexColumnPart() {
        IndexColumnImpl column = IndexColumnImpl.wrap(column("col1"));
        assertThat(sql(column), is("col1"));

        column = IndexColumnImpl.wrap(column("col1", SortOrder.ASC_NULLS_FIRST));
        assertThat(sql(column), is("col1 asc nulls first"));

        column = IndexColumnImpl.wrap(column("col1", SortOrder.DESC_NULLS_LAST));
        assertThat(sql(column), is("col1 desc nulls last"));
    }

    @Test
    void indexColumnParseSorted() {
        assertThat(parseColumn("col1"), is(column("col1", SortOrder.DEFAULT)));
        assertThat(parseColumn("COL2_UPPER_CASE ASC"), is(column("COL2_UPPER_CASE", SortOrder.ASC)));
        assertThat(parseColumn("col3 ASC    nUlls First  "), is(column("col3", SortOrder.ASC_NULLS_FIRST)));
        assertThat(parseColumn(" col4   asc  nulls  last "), is(column("col4", SortOrder.ASC_NULLS_LAST)));
        assertThat(parseColumn("col5 desc"), is(column("col5", SortOrder.DESC)));
        assertThat(parseColumn("col6 desc nulls first"), is(column("col6", SortOrder.DESC_NULLS_FIRST)));
        assertThat(parseColumn("col7 desc nulls last"), is(column("col7", SortOrder.DESC_NULLS_LAST)));
        assertThat(parseColumn("col8 nulls first"), is(column("col8", SortOrder.NULLS_FIRST)));
        assertThat(parseColumn("col9 nulls last"), is(column("col9", SortOrder.NULLS_LAST)));
    }

    @Test
    void indexColumnParseSortedWrongOrder() {
        assertThat(parseColumn("col1 nulls first asc"), is(column("col1", SortOrder.NULLS_FIRST)));
        assertThat(parseColumn("col2 nulls last desc"), is(column("col2", SortOrder.NULLS_LAST)));
        assertThat(parseColumn("col3 desc nulls"), is(column("col3", SortOrder.DESC)));
        assertThat(parseColumn("col4 desc last nulls"), is(column("col4", SortOrder.DESC)));
        assertThat(parseColumn("col5 nulls asc first"), is(column("col5")));
        assertThat(parseColumn("col6 first nulls"), is(column("col6")));
    }

    @Test
    void indexColumnPareIncorrectSortOrder() {
        assertThat(parseColumn("col1 unexpectedKeyword"), is(column("col1")));
        assertThat(parseColumn("col2 nulls_first"), is(column("col2")));
        assertThat(parseColumn("col3 descnullslast"), is(column("col3")));
    }

    private static String sql(QueryPart part) {
        return new QueryContext().visit(part).getSql();
    }
}
