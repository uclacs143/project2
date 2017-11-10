# Setup

## Development Environment

We **strongly** encourage you to use a Unix-like system (e.g. MacOS, Ubuntu, etc) as the Spark development environment. If you can only develop on Windows, we recommend you to use [Cygwin](https://www.cygwin.com/) rather than other Unix solutions on Windows to avoid most unexpected bugs.

It will be much more convenient to use an IDE for Spark development. [IntelliJ IDEA](https://www.jetbrains.com/idea/) (**recommended**) tends to be the most commonly used IDE for developing in Spark. IntelliJ is a Java IDE that has a Scala (and vim!) plugin. There are also other options such as [Scala-IDE](http://scala-ide.org) in Eclipse. Please note, TAs won't support issues related to IDEs.

We **discourage** using VM from project 1. If you absolutely must, please increase memory allocation for VM, run `sudo apt-get update` and `sudo apt-get install default-jre`.
Even that, using IDE with VM is tricky and TAs won't support issues related to virtual machines.


## Git and GitHub

`git` is a *version control* system, helping you track different versions of your code, synchronize them across different machines, and collaborate with others. [GitHub](https://github.com) is a site which supports this system, hosting it as a service.

If you don't know much about `git`, we *strongly recommend* you to familiarize yourself with this system; you'll be spending a lot of time with it!
There are many guides to using `git` online - [here](http://git-scm.com/book/en/v1/Getting-Started) is a great one to read.


## Setting up the Project

You should first clone the repository from our remote course project on Github

    $ cd ~
    $ git clone https://github.com/uclacs143/project2.git

NOTE: Please do not be overwhelmed by the amount of code that is here. Spark is a big project with a lot of features.

Once you have the pulled the code, you can build the project by entering to the cloned directory and make compile. **Please make sure you have JDK 7 or [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) installed on your platform before compiling the project.**

    $ cd project2
    $ make compile

The first time you run this command, it should take a while -- `sbt` will download all the dependencies and compile all the code in Spark (there's quite a bit of code). Once the initial assembly commands finish, you can start your project! (Future builds should not take this long -- `sbt` is smart enough to only recompile the changed files, unless you run `make clean`, which will remove all compiled class files.)

## Import Project in IntelliJ IDEA

If you use IntelliJ IDEA (Community version is enough), please make sure you have **Scala and SBT plugins installed** in IntelliJ IDEA. Import the whole project as **external SBT project** and keep other settings in default should work.

If you try to run your code within IntelliJ, please **disable** the 'Make' command before actually running the program. Code should be built from command line only (`make compile`).

If build fails for unknown reasons, be sure to try `make clean` and then do a `make compile` again.

## Searching files

The following UNIX command will come in handy, when you need to find the location of a file. Example - Find location of a file named 'DiskHashedRelation.scala' in my current directory:

    $ find ./ -name 'DiskHashedRelation.scala'

IDE shortcuts for finding a class: Ctrl/Cmd + O for IntelliJ, Ctrl/Cmd + Shift + T for Eclipse.

