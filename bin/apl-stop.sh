#!/bin/bash
# (C) 2019 Apollo Foundation 
# Stop apl-blockchain application, which was run by apl-start.sh script

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

 . ${DIR}/apl-common.sh


if [ -e ${APPLICATION}/apl.pid ]; then
    PID=`cat ${APPLICATION}/apl.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    echo "stopping"
    while [ $STATUS -eq 0 ]; do
        kill ${PID} > /dev/null
        echo "Trying to stop nicely PID ${PID}"
        sleep 5
        ps -p $PID > /dev/null
        STATUS=$?
        if [ $STATUS -eq 0 ] ; then
            echo "Forcing kill of PID $PID"
            kill -9 `cat ${APPLICATION}/apl.pid` > /dev/null 2>&1
        fi
        ps -p $PID > /dev/null
    done
    rm -f ${APPLICATION}/apl.pid
    echo "Apl server stopped"
else
    echo "No PID file found. May be progream is not running"
fi
