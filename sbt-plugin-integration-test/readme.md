# Integration Test Project

Integration testing [for the overall extraction project](https://github.com/CANVE/extractor). Runs the sbt plugin for all sbt projects located in `src/main/resources/integration-test-projects`, reporting execution for them.
<br> 

##Configuring to run the test
All projects locally present at src/main/resources/integration-test-projects when unleashing the integration test, will be used for the test. Technically, they are first automatically copied to target/scala-2.11/classes/integration-test-projects/ and used from there (so you can go there to tinker and rerun after a failure). A `clean` command will also clear them from `target/`.
<br>

To keep things modular as well as lightweight, [a separate git repository houses a recommended assortment of projects to test on](https://github.com/CANVE/extractor/tree/master/sbt-plugin-integration-test). So you typically want to pull that repo in to run the integration test. Simply cd into src/main/resources/integration-test-projects, and [follow the specific cloning instructions of the test projects repo](https://github.com/CANVE/integration-test-projects#cloning). 
<br>

Alternatively, you can skip that, and integration test on a different set of projects - in that case directly copy those projects to the same mentioned local location: src/main/resources/integration-test-projects.

##Running the test 

Make sure you have the java 8 version of javac and javadoc on your classpath. Some projects _require_ jdk8 or later, and this 
should in theory never matter for older projects.

Make sure the test will use the latest code through:
```
sbt +publishLocal
```

Then:
```
sbt integrationTest
```

 (or, if you are on this subproject in sbt, simply `run`, but in that case you still have to first `+publishLocal` from the root project one time.. )
