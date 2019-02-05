#!/bin/sh
# (C) 2019 Apollo Foundation 
# Starts Apollo GUI  in foreground

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh 


unamestr=`uname`
xdock=''

if [[ "$unamestr" == 'Darwin' ]]; then
  xdock=-Xdock:icon=./favicon.ico
fi
# uncomment when GUI will start standalone
# ${JAVA_CMD} $xdock  -jar ${MAIN_GUI_JAR}

${JAVA_CMD} $xdock  -Dapl.runtime.mode=desktop -jar ${MAIN_JAR}
