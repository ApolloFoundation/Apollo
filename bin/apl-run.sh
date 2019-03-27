#!/bin/bash
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground

export JAVA_HOME=/usr/local/zulu
export PATH=$JAVA_HOME/bin:$PATH

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

 . ${DIR}/apl-common.sh 

${JAVA_CMD} -jar ${MAIN_JAR} $@
