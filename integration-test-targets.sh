color='\e[1;34m'
detail_color='\e[1;30m'
NC='\e[0m' # No Color

echo -e ${color}"To rerun an integration test, or see its output files, cd or browse to the following directory:"
cd sbt-plugin-integration-test/target/scala-2.11/classes/integration-test-projects
echo file://`pwd`
echo -e ${detail_color}
echo "Each subdirectory there is a project used for the integration test, which you can <sbt canve> to rerun."
echo
ls -la
echo -e ${NC}
