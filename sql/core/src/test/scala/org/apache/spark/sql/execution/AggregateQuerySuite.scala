/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.execution

import org.apache.spark.sql.catalyst.errors.TreeNodeException
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.{QueryTest, SQLConf, TestData}
import org.scalatest.BeforeAndAfterAll

/* Implicits */
import org.apache.spark.sql.TestData._
import org.apache.spark.sql.test.TestSQLContext._

class AggregateQuerySuite extends QueryTest with BeforeAndAfterAll {
  // Make sure the tables are loaded.
  TestData

  // TESTS FOR TASK #4
  test("aggregation with codegen") {
    val originalValue = codegenEnabled
    setConf(SQLConf.CODEGEN_ENABLED, "true")
    sql("SELECT key FROM testData GROUP BY key").collect()
    setConf(SQLConf.CODEGEN_ENABLED, originalValue.toString)
  }

  test("agg") {
    checkAnswer(
      sql("SELECT a, SUM(b) FROM testData2 GROUP BY a"),
      Seq((1,3),(2,3),(3,3)))

    checkAnswer(
      sql("SELECT MAX(key) FROM testData"), 100
    )
  }

  test("aggregates with nulls") {
    checkAnswer(
      sql("SELECT MIN(a), MAX(a), AVG(a), SUM(a), COUNT(a) FROM nullInts"),
      (1, 3, 2, 6, 3) :: Nil
    )
  }

  test("average") {
    checkAnswer(
      sql("SELECT AVG(a) FROM testData2"),
      2.0)
  }

  test("count") {
    checkAnswer(
      sql("SELECT COUNT(*) FROM testData2"),
      testData2.count())
  }

  test("throw errors for non-aggregate attributes with aggregation") {
    def checkAggregation(query: String, isInvalidQuery: Boolean = true) {
      val logicalPlan = sql(query).queryExecution.logical

      if (isInvalidQuery) {
        val e = intercept[TreeNodeException[LogicalPlan]](sql(query).queryExecution.analyzed)
        assert(
          e.getMessage.startsWith("Expression not in GROUP BY"),
          "Non-aggregate attribute(s) not detected\n" + logicalPlan)
      } else {
        // Should not throw
        sql(query).queryExecution.analyzed
      }
    }
  }
}
