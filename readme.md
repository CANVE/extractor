[![Build Status](https://travis-ci.org/CANVE/extractor.svg?branch=master)](https://travis-ci.org/CANVE/extractor)

Extracts and normalizes the type relationships and call graph of scala sbt projects, through an sbt plugin which injects a compiler plugin and normalizes data across projects. Currently the project only publishes locally and you need to [drop us a line on gitter](https://gitter.im/CANVE/canve?utm_source=share-link&utm_medium=link&utm_campaign=share-link) to get the up-to-date getting started instructions.

### development notes:

This project currently uses [git submodules](https://git-scm.com/docs/git-submodule) to include some of its dependencies. You can think about git submodules as pointers to other repos. This means one more action is 
required to get a working local copy of the repo:

After cloning, cd into the cloned directory and run `git submodule update --init`. 
This will pull in the _"pointed at"_ projects, and now you have the full project
to work on locally.

License: TBD
