#!/bin/bash
# (C) 2019 Apollo Foundation 
# Stop apl-blockchain application, which was run by apl-start.sh script

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

 . ${DIR}/apl-common.sh

KILL_ATTEMPTS=3;
KILL_WAIT=5
if [ -e ${APPLICATION}/apl.pid ]; then
    PID=`cat ${APPLICATION}/apl.pid`
    ps -p $PID > /dev/null 
    STATUS=$?
    echo "stopping Apollo"
    i=0
    while [ $i -lt $KILL_ATTEMPTS ]; do
        kill ${PID} > /dev/null 2>&1
        echo "Trying to stop nicely PID ${PID} ( $i )"
        ps -p $PID > /dev/null
        STATUS=$?
        if [ $STATUS -ne 0 ] ; then
    	    break
        fi
        sleep $KILL_WAIT
        ps -p $PID > /dev/null
        STATUS=$?
        i=$((i+1))
    done
    
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ] ; then
        echo "Forcing kill of PID $PID"
        kill -9 `cat ${APPLICATION}/apl.pid` > /dev/null 2>&1    
    fi    
    rm -f ${APPLICATION}/apl.pid
    echo "Apollo server stopped"
else
    echo "No PID file found. May be Apollo is not running"
fi
