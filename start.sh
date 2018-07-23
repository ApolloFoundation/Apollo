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
nohup ${JAVA} -cp target/classes:target/lib/*:conf:addons/classes:addons/lib/* com.apollocurrency.aplwallet.apl.Apl > /dev/null 2>&1 &
echo $! > ~/.${APPLICATION}/apl.pid
cd - > /dev/null
