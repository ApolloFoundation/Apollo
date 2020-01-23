#!/bin/bash
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground
JAVA_OPT=${JAVA_OPT}" -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

 . ${DIR}/apl-common.sh 

${JAVA_CMD} ${JAVA_OPT} -jar ${MAIN_JAR} $@
