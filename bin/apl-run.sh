#!/bin/bash
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

 . ${DIR}/apl-common.sh 

${JAVA_CMD} ${JAVA_OPT} -jar ${MAIN_JAR} $@
