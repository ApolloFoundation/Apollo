#!/bin/sh
APPLICATION="apl-clone"
if [ -e ~/.${APPLICATION}/apl.pid ]; then
    PID=`cat ~/.${APPLICATION}/apl.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Apl server already running"
        exit 1
    fi
fi
mkdir -p ~/.${APPLICATION}/
DIR=`dirname "$0"`
cd "${DIR}"
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
nohup ${JAVA} -cp addons/classes:addons/lib/* -Dapl.runtime.mode=desktop -jar Apollo.jar > /dev/null 2>&1 &
echo $! > ~/.${APPLICATION}/apl.pid
cd - > /dev/null
