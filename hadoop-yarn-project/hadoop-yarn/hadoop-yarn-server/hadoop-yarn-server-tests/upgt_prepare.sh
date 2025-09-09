#!/bin/bash

cur_version=$1

if [ -z "$cur_version" ]; then
	echo "Usage: $0 <cur_version>"
	exit 1
fi

# remove the upgrade directory if it exists
rm -rf upgrade/

# Ensure the destination directory exists
mkdir -p upgrade/target
mkdir -p upgrade/jars

# Copy the directories target/classes and target/test-classes to upgrade/target/
cp -r target/classes upgrade/target/
cp -r target/test-classes upgrade/target/

# Recursively remove any .java and .class files in upgrade/target/
find upgrade/target/ -type f \( -name "*.java" -o -name "*.class" \) -delete


# Copy Jar files to the current directory
cp target/*jar upgrade/jars

# Create a classpath file for the current version
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
cp cp.txt upgrade/jars/${cur_version}-cp.txt
