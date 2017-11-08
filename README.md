# Fall 2017 CS 143 Project 2

This project is split into two parts. In Part A, you'll implement the caching mechanism of User-Defined Functions. In Part B, you'll implement the hash-based aggregation mechanism. Part A has 4 tasks and Part B has 3 tasks (the last 2 are extra credits).

For all tasks, you will need to implement the required functionalities on Apache Spark, a leading distributed computing framework, using Scala programming language. The project is based on Spark version: 1.3.0-SNAPSHOT, thus some functionalities may not conform to the most recent Spark documentation.

To setup the project on your computer, please check [setup.md](setup.md). For more information about Spark and Scala, please check [spark.md](spark.md).


# PART A

User-Defined Functions (UDFs) allow developers to define and exploit custom operations within expressions. For instance, say that you have a product catalog that includes photos of the product packaging. You may want to register a user-defined function `extract_text` that calls an OCR algorithm and returns the text in an image, so that you can get 'queryable' information out of the photos. You can do this in SQL easily. In SQL, you could imagine a query like this:
    
    SELECT P.name, P.manufacturer, P.price, extract_text(P.image), 
      FROM Products P;
     
The ability to register UDFs is very powerful -- it extends the ability of your data processing framework. Now the question is, what are the difficulties that one might face when we implement UDFs in distributed environments? For this, lets look at [Apache Spark](http://spark.apache.org). Apache Spark is the leading distributed computing framework. Spark SQL, built on top of Spark is one of its popular components. As it turns out, you can implement and register UDFs in Spark SQL as well. But UDFs can often introduce performance bottlenecks, especially as we run them over millions of data items.

To complete this part, you will need to develop a user-defined function (UDF) that provides the following functionality in an efficient and reliable manner: for the two situations when (i) all the values can fit in memory and (ii) when they do not fit in memory. As you can guess, hashing will be involved. More precisely, you will work in this project to achieve the following goals:

## Assignment Goals

1. *Implement disk hash-partitioning*
2. *Implement in-memory UDF caching*
3. *Implement hash-partitioned UDF caching*


This project will illustrate key concepts in data rendezvous and query evaluation, and you'll get some hands-on experience modifying Spark, which is widely used in the field. In addition, you'll get exposure to Scala, a JVM-based language that is gaining popularity for its clean functional style.

Lastly, there is a lot of code in this directory. DO NOT GET OVERWHELMED!!
The code that we will be touching is in [sql/core/src/main/scala/org/apache/spark/sql/execution/](sql/core/src/main/scala/org/apache/spark/sql/execution/).
The tests will all be contained in [sql/core/src/test/scala/org/apache/spark/sql/execution/](sql/core/src/test/scala/org/apache/spark/sql/execution/).


## Background and Framework

### UDFs (**U**ser **D**efined **F**unctions)

	SELECT P.name, P.manufacturer, P.price, extract_text(P.image), 
	  FROM Products P;
	 
Let's look at the above example, which we used earlier.
If the input column(s) to a UDF contain many duplicate values, it may be beneficial to improve performance by ensuring that the UDF is only called once per *distinct input value*, rather than once per *row*.  (For example in our Products example above, all the different configurations of a particular PC might have the same image.) In this assignment, we will implement this optimization.  We'll take it in stages -- first get it working for data that fits in memory, and then later for larger sets that require an out-of-core approach. We will use external hashing as the technique to "rendezvous" all the rows with the same input values for the UDF.

1. Implement disk-based hash partitioning.
1. Implement in-memory UDF caching.
1. Combine the above two techniques to implement out-of-core UDF caching.

If you're interested in the topic, the following paper will be an interesting read (optional).

* [Query Execution Techniques for Caching Expensive Methods (SIGMOD 96)](http://db.cs.berkeley.edu/cs286/papers/caching-sigmod1996.pdf) 


### Project Framework

All the code you will be touching will be in three files -- `CS143Utils.scala`, `basicOperators.scala`, and `DiskHashedRelation.scala`. You might however need to consult other files within Spark or the general Scala APIs in order to complete the assignment thoroughly. Please make sure you look through *all* the provided code in the three files mentioned above before beginning to write your own code. There are a lot of useful functions in `CS143Utils.scala` as well as in `DiskHashedRelation.scala` that will save you a lot of time and cursing -- take advantage of them!

In general, we have defined most (if not all) of the methods that you will need. As before, in this project, you need to fill in the skeleton. The amount of code you will write is not very high -- the total staff solution is less than a 100 lines of code (not including tests). However, stringing together the right components in a memory-efficient way (i.e., not reading the whole relation into memory at once) will require some thought and careful planning.

### Some Terminology Differences

There are some potentially confusing differences between the common terminology, and the terminology used in the SparkSQL code base:

* The "iterator" concept we normally use is called a "node" in the SparkSQL code -- there are definitions in the code for UnaryNode and BinaryNode.  A query plan is called a SparkPlan, and in fact UnaryNode and BinaryNode extend SparkPlan (after all, a single iterator is a small query plan!)  You may want to find the file `SparkPlan.scala` in the SparkSQL source to see the API for these nodes.

* In some of the comments in SparkSQL, they also use the term "operator" to mean "node".  The file `basicOperators.scala` defines a number of specific nodes (e.g. Sort, Distinct, etc.).

* Don't confuse the [Scala interface `Iterator`](http://www.scala-lang.org/api/current/index.html#scala.collection.Iterator) with the iterator concept used otherwise. The `Iterator` that you will be using in this project is a Scala language feature that you will use to implement your SparkSQL nodes.  `Iterator` provides an interface to Scala collections that enforces a specific API: the `next` and `hasNext` functions. 

# Your Task

## Disk hash-partitioning

We have provided you skeleton code for `DiskHashedRelation.scala`. This file has 4 important things:
* `trait DiskHashedRelation` defines the DiskHashedRelation interface
* `class GeneralDiskHashedRelation` is our implementation of the `DiskedHashedRelation` trait
* `class DiskPartition` represents a single partition on disk
* `object DiskHashedRelation` can be thought of as an object factory that constructs `GeneralDiskHashedRelation`s

### Task #1: Implementing `DiskPartition` and `GeneralDiskHashedRelation`

First, you will need to implement the `insert`, `closeInput`, and `getData` methods in `DiskPartition` for this part. For the former two, the docstrings should provide a comprehensive description of what you must implement. The caveat with `getData` is that you *cannot* read the whole partition into memory in once. The reason we are enforcing this restriction is that there is no good way to enforce freeing memory in the JVM, and as you transform data to different forms, there would be multiple copies lying around. As such, having multiple copies of a whole partition would cause things to be spilled to disk and would make us all sad. Instead, you should stream *one block* into memory at a time.

At this point, you should be passing the tests in `DiskPartitionSuite.scala`.

### Task #2: Implementing `object DiskHashedRelation`

Your task in this portion will be to implement phase 1 of external hashing -- using a coarse-grained hash function to stream an input into multiple partition relations on disk. For our purposes, the `hashCode` method that every object has is sufficient for generating a hash value, and taking the modulo by the number of the partitions is an acceptable hash function. 

At this point, you should be passing all the tests in `DiskHashedRelationSuite.scala`.

## In-Memory UDF Caching

In this section, we will be dealing with `case class CacheProject` in `basicOperators.scala`. You might notice that there are only 4 lines of code in this class and, more importantly, no `/* IMPLEMENT THIS METHOD */`s. You don't actually have to write any code here. However, if you trace the function call in [line 66](sql/core/src/main/scala/org/apache/spark/sql/execution/basicOperators.scala#L66), you will find that there are two parts of this stack you must implement in order to have a functional in-memory UDF implementation.

### Task #3: Implementing `CS143Utils` methods

For this task, you will need to implement `getUdfFromExpressions` and the `Iterator` methods in `CachingIteratorGenerator#apply`. Please read the docstrings -- especially for `apply` -- closely before getting started.

After implementing these methods, you should be passing the tests in `CS143UtilsSuite.scala`.

Hint: Think carefully about why these methods might be a part of the Utils

## Disk-Partitioned UDF Caching

Now comes the moment of truth! We've implemented disk-based hash partitioning, and we've implemented in-memory UDF caching -- what is sometimes called [memoization](http://en.wikipedia.org/wiki/Memoization). Memoization is very powerful tool in many contexts, but here in databases-land, we deal with larger amounts of data than memoization can handle. If we have more unique values than can fit in an in-memory cache, our performance will rapidly degrade. 
Thus, we fall back to the time-honored databases tradition of divide-and-conquer. If our data does not fit in memory, then we can partition it to disk once, read one partition in at a time (think about why this works (hint: rendezvous!)), and perform UDF caching, evaluating one partition at a time. 

### Task #4: Implementing `PartitionProject`

This final task requires that you fill in the implementation of `PartitionProject` in `basicOperators.scala`. All the code that you will need to write is in the `generateIterator` method. Think carefully about how you need to organize your implementation. You should *not* be buffering all the data in memory or anything similar to that.

At this point, you should be passing ***all*** given tests.


## Testing

We have provided you some sample tests in `DiskPartitionSuite.scala`, `DiskHasedRelationSuite.scala`, `CS143UtilsSuite.scala` and `ProjectSuite.scala` for Part A and in `InMemoryAggregateSuite`, `SpillableAggregationSuite` and `RecursiveAggregationSuite` for Part B. These tests can guide you as you complete this project. However, keep in mind that they are *not* comprehensive, and you are well advised to write your own tests to catch bugs. Hopefully, you can use these tests as models to generate your own tests. 

In order to run our tests, we have provided a simple Makefile. In order to run the tests for task 1, run `make t1`. Correspondingly for task, run `make t2`, and the same for all other tests. `make partA` will run all the tests for Part A and `make all` will run all the tests. 

# Part B

## Assignment Goals

1. *Implement hash-based aggregation*
2. *Implement spillable data strucuture*
3. *Combine the above two tecniques to implement hybrid hashing aggregation*

## Project Framework

All the code you will be touching will be in two files -- `CS143Utils.scala` and `SpillableAggregate.scala`. You might however need to consult other files within Spark (especially `Aggregate.scala`) or the general Scala APIs in order to complete the assignment thoroughly.

In general, we have defined most (if not all) of the methods that you will need. As before, in this project, you need to fill in the skeleton. The amount of code you will write is not very high -- the total staff solution is less than a 100 lines of code (not including tests). However, stringing together the right components in an efficient way will require some thought and careful planning.


# Your Task

## In memory hash-based aggregation

We have provided you skeleton code for `SpillableAggregate.scala`. This file has 4 important things:
* `aggregator` extracts the physical aggregation from the logical plan;
* `aggregatorSchema` contains the output schema for the aggregate at hand
* the method `newAggregatorInstance` creates the actual instance of the physical aggregator that will be used during execution
* `generateIterator` is the main method driving the computation of the aggregate.

### Task #5: Implementing the in-memory part of `SpillableAggregate`

First, you will need to implement the `aggregator`, `aggregatorSchema`, and `newAggregatorInstance` methods in `SpillableAggregate.scala` for this part. This is a simple exercise, just check `Aggregate.scala`. Try however to understand the logic because you will need those methods to implement `generateIterator`.
In order to complete the implementation of `generateIterator` at this point, you will need to fill the `hasNext`, `next`, `aggregate` and the `object AggregateIteratorGenerator` in `CS143Utils`. The logic of `generateIterator` is the following: 1) drain the input iterator into the aggregation table;
2) generate an aggregate iterator using the helper function `AggregateIteratorGenerator` properly formatting the aggregate result; and
3) use the Iterator inside `generateIterator` as external interface to access and drive the aggregate iterator. 

No need to spill to disk at this point.

## Hybrid hash aggregation

**Please make sure you have finished Task #1 to #5 and passed all the tests mentioned** before starting to work on extra credit tasks.

### [Extra Credit] Task #6: Make the aggregate table spill to disk

Your task is first to take a look at the `maybeSpill` method in `CS143Utils` (We have already filled this method for you). The next step is to revise your implementation of `generateIterator` in `SpillableAggregate.scala` by making the current aggregation table check if it can safely add a new record without triggering the spilling to disk. If the record cannot be added to the aggregation table, it will be spilled to disk. To implement the above logic, you will have to fill up the methods `initSpills` and `spillRecord`, and properly modify your implementation of `aggregate`. In `initSpills` remember to set blockSize to 0, otherwise spilled records will stay in memory and not actually spill to disk!

### [Extra Credit] Task #7: Recursive Aggregation

At this point the only missing part for having an hybrid-hash aggregate is to recursively aggregate records previously spilled to disk. If you have implemented the previous task correctly, to finish this task you will only have to take care of the situation in which 1) the input iterator of `generateIterator` is drained; 2) aggregate table contains aggregate values for a subset of the groups; and 3) the remaining groups sit on disk in files properly partitioned. The idea now is to clear the current aggregation table and fetch the spilled records partition by partition and aggregate them in memory. Implement `fetchSpill` and revise your implementation of `hasNext` and `next` in `generateIterator`. You can assume that the aggregate table for each partition fits in memory (what would you do if instead we remove this assumption?).


# Assignment Submission

Submission link will be created on [CCLE](https://ccle.ucla.edu/course/view/17F-COMSCI143-2), where you can submit your code by the due date. In project root directory, please create the `team.txt` file which contains the UID(s) of every member of your team.

After that, please run following commands to create the submission zip archive.

    $ chmod +x package.sh
    $ ./package.sh

Please **only submit** the script-created `project2.zip` file to CCLE.


# Acknowledgements

Big thanks to Matteo Interlandi.

**Good luck!**
