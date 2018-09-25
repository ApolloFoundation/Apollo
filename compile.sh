#!/bin/sh
CP="target/lib/*:target/classes"
SP=src/main/java/
# first param is a profile for building project via maven or another custom option specified like -Dmaven.test.skip=true
echo "compiling and packaging Apollo wallet..."
if [ -z "$1" ]; then
    mvn clean package -Dmaven.test.skip=true
else
    mvn clean package -Dmaven.test.skip=true "$1"
fi

echo "Apollo wallet was compiled successfully"

rm -f "Apollo.jar"
rm -f -r "lib"
cp -f "target/Apollo.jar" "Apollo.jar"
cp -r "target/lib/" "lib/"

find addons/src/ -name "*.java" > addons.tmp
if [ -s addons.tmp ]; then
    echo "compiling add-ons..."
    javac -encoding utf8 -sourcepath "${SP}:addons/src" -classpath "${CP}:addons/classes:addons/lib/*" -d addons/classes @addons.tmp || exit 1
    echo "add-ons compiled successfully"
    rm -f addons.tmp
else
    echo "no add-ons to compile"
    rm -f addons.tmp
fi

echo "compilation done"
