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
import org.apache.spark.util.collection.SizeTrackingAppendOnlyMap
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.collection.mutable.ArraySeq

class SpillableAggregationSuite extends FunSuite with BeforeAndAfterAll {
  var list: ArraySeq[(Int, Int)] = null

   // TESTS FOR TASK #2
   test("maybeSpill") {
     list = new ArraySeq[(Int, Int)](44)

     for (i <- 0 to 43) {
       list(i) = ((i, (i % 4) + 1))
     }

     val currentAggregationTable = new SizeTrackingAppendOnlyMap[Int, Int]
     list.foreach(r => {
       currentAggregationTable.update(r._1, r._2)
     })

     assert(!CS143Utils.maybeSpill(currentAggregationTable, 4000))

     currentAggregationTable.update(44, 5)
     assert(CS143Utils.maybeSpill(currentAggregationTable, 4000))
   }

   test("spillable aggregate") {
     list = new ArraySeq[(Int, Int)](200)

     for (i <- 0 until 200) {
       list(i) = ((i, (i % 4) + 1))
     }

     val attributes: Seq[Attribute] = ScalaReflection.attributesFor[Student]
     val data = TestSQLContext.sparkContext.parallelize(list.map(r => Row(r._1, r._2)), 1)
     val plan: SparkPlan = PhysicalRDD(attributes, data)

     val exp: Expression = count(attributes(1))

     val aggregate = new SpillableAggregate(false, Seq(attributes(0)), Seq(attributes(0), Alias(exp, exp.toString)()), plan)
     val answer = plan.execute().mapPartitions(iter => aggregate.generateIterator(iter))
     assert(answer.map(r => r.getInt(0)).reduce(Math.max(_, _)) == 179)
   }
 }
