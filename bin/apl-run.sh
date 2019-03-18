#!/bin/bash
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground

export JAVA_HOME=/usr/local/zulu
export PATH=$JAVA_HOME/bin:$PATH

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh 

${JAVA_CMD} -jar ${MAIN_JAR} $@
