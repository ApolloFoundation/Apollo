#!/bin/sh
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground
# This file required for starting application in service mode on Linux/MacOs
# Also required for Linux/MacOs installer

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh 

${JAVA_CMD} -jar ${MAIN_JAR}
