#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

cd $DIR

if [ -f "project2.zip" ]; then
  echo "[Error] $DIR/project2.zip exists, remove it before running the script"
  exit 1
fi

if [ -d "submission" ]; then
  echo "[Error] $DIR/submission directory exists, remove it before running the script"
  exit 2
fi

if [ ! -f "team.txt" ]; then
  echo "[Error] Lack $DIR/team.txt"
  exit 3
fi

echo "[Info] Make sure you have done following before making the submission zip."
echo ""
echo "1. Run 'make all' to pass all tests"
echo "2. Correctly put uid(s) in 'team.txt'"
echo "3. Script will only package 'team.txt' and 'sql/core/src/main/scala/org/apache/spark/sql/execution' directory"
echo ""
read -p "Press enter to continue ..."

mkdir submission &&
cp team.txt submission/team.txt &&
cp -r sql/core/src/main/scala/org/apache/spark/sql/execution submission/execution &&
zip -r project2.zip submission &&
rm -rf submission

echo ""
echo "[Success] Created '$DIR/project2.zip', please submit it to CCLE."