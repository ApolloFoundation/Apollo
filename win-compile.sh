#!/bin/sh
CP="target/lib/*;target/classes"
SP=src/main/java/
APPLICATION="apl-clone"

/bin/rm -f ${APPLICATION}.jar
/bin/rm -f ${APPLICATION}service.jar
/bin/rm -rf classes
/bin/mkdir -p classes/
/bin/rm -rf addons/classes
/bin/mkdir -p addons/classes/
echo "Compiling main APL classes"

mvn clean package -Dmaven.test.skip=true && echo Main classes compiled successfully || echo Classes cannot be compiled & exit 1

rm -f "Apollo.jar"
rm -f -r "lib"
cp -f "target/Apollo.jar" "Apollo.jar"
cp -r "target/lib/" "lib/"

ls addons/src/*.java > /dev/null 2>&1 || exit 0
javac -encoding utf8 -sourcepath "${SP}" -classpath "${CP}" -d addons/classes addons/src/*.java || exit 1

echo "addon class files compiled successfully"
