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
import org.apache.spark.sql.catalyst.expressions.{Expression, Attribute, ScalaUdf, Row}
import org.apache.spark.sql.catalyst.types.IntegerType
import org.scalatest.FunSuite

import scala.collection.mutable.ArraySeq
import scala.util.Random



class CS143UtilsSuite extends FunSuite {
  val numberGenerator: Random = new Random()

  val studentAttributes: Seq[Attribute] =  ScalaReflection.attributesFor[Student]

  // TESTS FOR TASK #3
  /* NOTE: This test is not a guarantee that your caching iterator is completely correct.
     However, if your caching iterator is correct, then you should be passing this test. */
  test("caching iterator") {
    val list: ArraySeq[Row] = new ArraySeq[Row](1000)

    for (i <- 0 to 999) {
      list(i) = (Row(numberGenerator.nextInt(10000), numberGenerator.nextFloat()))
    }


    val udf: ScalaUdf = new ScalaUdf((sid: Int) => sid + 1, IntegerType, Seq(studentAttributes(0)))

    val result: Iterator[Row] = CachingIteratorGenerator(studentAttributes, udf, Seq(studentAttributes(1)), Seq(), studentAttributes)(list.iterator)

    assert(result.hasNext)

    result.foreach((x: Row) => {
      val inputRow: Row = Row(x.getInt(1) - 1, x.getFloat(0))
      assert(list.contains(inputRow))
    })
  }

  test("sequence with 1 UDF") {
    val udf: ScalaUdf = new ScalaUdf((i: Int) => i + 1, IntegerType, Seq(studentAttributes(0)))
    val attributes: Seq[Expression] = Seq() ++ studentAttributes ++ Seq(udf)

    assert(CS143Utils.getUdfFromExpressions(attributes) == udf)
  }
}
