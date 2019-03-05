#!/bin/sh
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in background
# Required for Linux/MacOs installer.

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh

if [ -e ~/${APPLICATION}/apl.pid ]; then
    PID=`cat ~/${APPLICATION}/apl.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Apl server already running"
        exit 1
    fi
fi

nohup ${JAVA_CMD} -jar ${MAIN_JAR} > /dev/null 2>&1 &
echo $! > ~/${APPLICATION}/apl.pid
cd - > /dev/null
