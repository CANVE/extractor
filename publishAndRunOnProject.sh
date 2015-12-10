sbt +publishLocal
cd "sbt-plugin-integration-test/target/scala-2.11/classes/integration-test-projects/"$1
rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi
echo "Running for target project "$1"..."
sbt canve # > out.txt && subl out.txt
