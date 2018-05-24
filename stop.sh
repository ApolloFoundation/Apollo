#!/bin/sh
APPLICATION="apl-clone"
if [ -e ~/.${APPLICATION}/apl.pid ]; then
    PID=`cat ~/.${APPLICATION}/apl.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    echo "stopping"
    while [ $STATUS -eq 0 ]; do
        kill `cat ~/.${APPLICATION}/apl.pid` > /dev/null
        sleep 5
        ps -p $PID > /dev/null
        STATUS=$?
    done
    rm -f ~/.${APPLICATION}/apl.pid
    echo "Apl server stopped"
fi

