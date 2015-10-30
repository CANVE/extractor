package extractor

/*
 *  invoke SBT on all projects under test-projects/ - not implemented
 */
class RunOnExternalSBT {
  // from the command line this would be: 
  // sbt "set scalacOptions in ThisBuild += \"-Xplugin:/home/matan/.ivy2/local/matanster/extractor_2.11/0.0.1/jars/extractor_2.11.jar\"" clean compile
  // after changing dir into the specific project
}