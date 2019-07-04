#!/bin/bash
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in background
# Required for Linux/MacOs installer.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

 . ${DIR}/apl-common.sh

if [[ ! -d "${APPLICATION}" ]] ; then
  mkdir -p  ${APPLICATION}
fi

if [ -e ${APPLICATION}/apl.pid ]; then
    PID=`cat ${APPLICATION}/apl.pid`
    ps -p $PID > /dev/null
    STATUS=$?
    if [ $STATUS -eq 0 ]; then
        echo "Apl server already running"
        exit 1
    fi
fi

nohup ${JAVA_CMD} ${JAVA_OPT} -jar ${MAIN_JAR} $@ > /dev/null 2>&1 &

#cd - > /dev/null
