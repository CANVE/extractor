sbt +publishLocal
rc=$?; if [[ $rc != 0 ]]; then exit $rc; fi
cd "integration/"$1
echo "Running for target project "$1"..."
sbt canve | tee out.txt
echo "Run output saved in file:///"`pwd`"/out.txt"
