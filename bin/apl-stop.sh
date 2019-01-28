#!/bin/sh
# (C) 2019 Apollo Foundation 
# Stop apl-blockchain application, which was run by apl-start.sh script

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh


if [ -e ~/${APPLICATION}/apl.pid ]; then
    PID=`cat ~/${APPLICATION}/apl.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    echo "stopping"
    while [ $STATUS -eq 0 ]; do
        kill `cat ~/${APPLICATION}/apl.pid` > /dev/null
        sleep 5
        ps -p $PID > /dev/null
        STATUS=$?
    done
    rm -f ~/${APPLICATION}/apl.pid
    echo "Apl server stopped"
fi

