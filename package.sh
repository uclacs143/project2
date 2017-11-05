#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR

if [ ! -f team.txt ];
then
  echo "Lack $DIR/team.txt"
  exit 1
fi

echo "Make sure you have done following before making the submission zip."
echo ""
echo "1. Run 'make all' to pass all tests"
echo "2. Correctly put uid(s) in team.txt"
echo "3. No creation of any new scala files as they will not be zipped"
echo ""
read -p "Press enter to continue ..."

zip -r project2.zip sql/core/src/main/scala/org/apache/spark/sql/execution

echo ""
echo "Successfully created '$DIR/project2.zip', please submit it to CCLE."