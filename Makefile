SBT=build/sbt
TEST=sql/test:testOnly
T1=org.apache.spark.sql.execution.DiskPartitionSuite
T2=org.apache.spark.sql.execution.DiskHashedRelationSuite
T3=org.apache.spark.sql.execution.CS143UtilsSuite
T4=org.apache.spark.sql.execution.ProjectSuite
T5=org.apache.spark.sql.execution.InMemoryAggregateSuite
T6=org.apache.spark.sql.execution.SpillableAggregationSuite
T7=org.apache.spark.sql.execution.RecursiveAggregationSuite

compile:
	$(SBT) compile

clean:
	$(SBT) clean

partA:
	$(SBT) "$(TEST) $(T1) $(T2) $(T3) $(T4)"

partB:
	$(SBT) "$(TEST) $(T5) $(T6) $(T7)"

all:
    $(SBT) "$(TEST) $(T1) $(T2) $(T3) $(T4) $(T5) $(T6) $(T7)"

t1:
	$(SBT) "$(TEST) $(T1)"

t2:
	$(SBT) "$(TEST) $(T2)"

t3:
	$(SBT) "$(TEST) $(T3)"

t4:
	$(SBT) "$(TEST) $(T4)"

t5:
	$(SBT) "$(TEST) $(T4)"

t6:
	$(SBT) "$(TEST) $(T4)"

t7:
	$(SBT) "$(TEST) $(T4)"
