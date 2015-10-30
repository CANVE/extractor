# Integration Test Project

Integration testing for the [overall extraction project](https://github.com/CANVE/extractor). Runs the sbt plugin for all sbt projects located in `src/main/resources/integration-test-projects`, reporting execution for them.

##Configuring the test
All projects locally present at `src/main/resources/integration-test-projects` when the test is run, are used for the test. 
<br>

To keep things modular as well as lightweight, a repo containig a recommended assortment of projects to test on, is managed as a separate git repo. so you typically want to pull that repo in to run the test. Simply cd into `src/main/resources/integration-test-projects`, and [follow the specific cloning instructions of the test projects repo](https://github.com/CANVE/integration-test-projects#cloning). 
<br>

Alternatively, you can skip that, and integration test on a different set of projects - in that case directly copy those projects to the same location (`src/main/resources/integration-test-projects`).

##Running the test

```sbt run```
