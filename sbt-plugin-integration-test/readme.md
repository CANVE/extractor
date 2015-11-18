# Integration Test Project

Integration testing [for the overall extraction project](https://github.com/CANVE/extractor). Runs the sbt plugin for all sbt projects located in `src/main/resources/integration-test-projects`, reporting execution for them.
<br> 

##Configuring the test
All projects locally present inn the resources directory src/main/resources/integration-test-projects when the test is run, are used for the test. 
<br>

To keep things modular as well as lightweight, [a separate git repository houses a recommended assortment of projects to test on](https://github.com/CANVE/extractor/tree/master/sbt-plugin-integration-test). So you typically want to pull that repo in to run the integration test. Simply cd into src/main/resources/, and [follow the specific cloning instructions of the test projects repo](https://github.com/CANVE/integration-test-projects#cloning). 
<br>

Alternatively, you can skip that, and integration test on a different set of projects - in that case directly copy those projects to: src/main/resources/integration-test-projects.

##Running the test 

Make sure the test will use the latest code with:
```
sbt +publishLocal
```

Then:
```
sbt integrationTest
```

 (or, if you are on this subproject in sbt, simply `run`, but in that case you still have to first `+publishLocal` from the root project one time.. )

##A small note
This integration test is using the "resources" api. This means that the test projects directory is copied to the `target` directory by the runtime  - and served to the test code from there. The exact location of this automatic copy would be: `target/scala-x.xx/classes/`.
