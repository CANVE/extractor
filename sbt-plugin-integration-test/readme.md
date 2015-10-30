# Integration Testing Project

Integration testing for the [overall extraction project](https://github.com/CANVE/extractor). Runs the sbt plugin for all sbt projects located in `src/main/resources/test-projects`, reporting their execution.

All projects present at `src/main/resources/test-projects` are used for the testing. To keep things lightweight, they are managed as [git submodules](https://git-scm.com/docs/git-submodule), which means they are not physically included in this repo, but rather they are pointed at by it. See below how to run the tests.


##Running

First off, to physically pull in all [test projects linked by this repo](https://github.com/CANVE/extractor/tree/master/sbt-plugin-test-lib/src/main/resources/test-projects), use the command below before running for the first time, as they are not automatically brought in by virtue of merely cloning this repo.
```
git submodule update --init
```
Otherwise, simply `sbt run`.

##Updating all test projects to their latest
Well, say you want to run the integration test on the latest online version of the test projects, not those that you pulled a couple of months ago. Before going on, it might make sense first testing as is, to isolate problems. But then you can ultimately update all the test projects to their latest online version, via:
```
git submodule foreach --recursive git pull
```

<br>

---

<br>

##Maintaining the inventory of test projects
Okay, so you want to add or remove a test project to the list.

####Adding a publicly available project as a test project

1. Make sure the project you are going to add is adequately licensed.

2. `git submodule add <GitLink> src/main/resources/test-projects/<ProjName>`. 

    For example -
    ```
    git submodule add git@github.com:allenai/pipeline.git src/main/resources/test-projects/pipeline
    ```

<br>

####Removing a test project from the repo

TBD

<br>
####Ad-hoc manipulation

You can add and remove projects as fits your situation - anything you drop into the directory will be treated as a test project. This won't magically update the repo, nor keep the submodule registration in sync! so to avoid trouble, TBD...

