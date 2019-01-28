#!/bin/sh
# This script is used for starting application with desktop UI on clients machines.
# Required for Linux/MacOS installer
# Possible duplicate of apl-desktop.sh
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
