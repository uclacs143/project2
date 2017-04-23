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

import org.apache.spark.sql.catalyst.expressions.{Projection, Row}
import org.scalatest.FunSuite

import scala.collection.mutable.ArraySeq

class DiskHashedRelationSuite extends FunSuite {

  private val keyGenerator = new Projection {
    override def apply(row: Row): Row = row
  }

  // TESTS FOR TASK #2
  test("values are in correct partition") {
    val data: Array[Row] = (0 to 100).map(i => Row(i)).toArray
    val hashedRelation: DiskHashedRelation = DiskHashedRelation(data.iterator, keyGenerator, 3, 64000)
    var count: Int = 0

    for (partition <- hashedRelation.getIterator()) {
      for (row <- partition.getData()) {
        assert(row.hashCode() % 3 == count)
      }
      count += 1
    }
  }

   test ("empty input") {
    val data: ArraySeq[Row] = new ArraySeq[Row](0)
    val hashedRelation: DiskHashedRelation = DiskHashedRelation(data.iterator, keyGenerator)

    for (partition <- hashedRelation.getIterator()) {
      assert(!partition.getData.hasNext)
    }

    hashedRelation.closeAllPartitions()
  }
}