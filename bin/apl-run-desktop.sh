#!/bin/sh
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi

unamestr=`uname`
xdock=''

if [[ "$unamestr" == 'Darwin' ]]; then
  xdock=-Xdock:icon=./favicon.ico
fi

${JAVA} $xdock -Dapl.runtime.mode=desktop -cp addons/classes:addons/lib/* -jar Apollo.jar
