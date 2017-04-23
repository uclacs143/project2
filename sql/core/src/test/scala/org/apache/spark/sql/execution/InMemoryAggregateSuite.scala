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

import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.expressions.{Alias, Row, _}
import org.apache.spark.sql.test.TestSQLContext
import org.scalatest.{BeforeAndAfter, FunSuite}

import scala.collection.mutable.ArraySeq

case class Student(sid: Int, gpa: Int)

class InMemoryAggregateSuite extends FunSuite with BeforeAndAfter {
  val list: ArraySeq[Row] = new ArraySeq[Row](10)

  for (i <- 0 to 9) {
    list(i) = (Row(i, (i % 4) + 1))
  }

  val attributes: Seq[Attribute] = ScalaReflection.attributesFor[Student]
  val data = TestSQLContext.sparkContext.parallelize(list, 1)
  val plan: SparkPlan = PhysicalRDD(attributes, data)

  // TESTS FOR TASK #1
  test("empty grouping aggregate") {
    val exp: Expression = count(1)
    val agg = Alias(exp, exp.toString)()

    val aggregate = new SpillableAggregate(false, Nil, Seq(Alias(exp, exp.toString)()), plan)
    val answer = aggregate.execute().collect().toSeq
    assert(answer.toSeq.map(r => r.getLong(0)) == Seq(10L))
  }

  test("nonempty grouping aggregate") {
    val exp: Expression = count(attributes(0))

    val aggregate = new SpillableAggregate(false, Seq(attributes(1)), Seq(attributes(1), Alias(exp, exp.toString)()), plan)
    val answer = aggregate.execute().collect().toSeq
    assert(answer.map(r => (r.getInt(0), r.getLong(1))).sortBy(_._1) == Seq((1, 3L), (2, 3L), (3, 2L), (4, 2L)))
  }
}
