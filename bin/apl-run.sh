#!/bin/sh
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh 

${JAVA_CMD} -jar ${MAIN_JAR}
