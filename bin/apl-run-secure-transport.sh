#!/bin/bash
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh 

# WARNING: java still bypasses the tor proxy when sending DNS queries and
# this can reveal the fact that you are running Apl, however blocks and
# transactions will be sent over tor only. Requires a tor proxy running
# at localhost:9050. Set apl.shareMyAddress=false when using tor.
# Run secure transport on Linux/MacOs. Required for Linux/MacOs installer.

#unamestr=`uname`
#if [[ "$unamestr" == 'Linux' ]]; then

#    cd secureTransport
    sudo ${APL_TOP_DIR}/secureTransport/runClient.sh 
    cd ..
#fi

xdock=''

if [[ "$unamestr" == 'Darwin' ]]; then
  xdock=-Xdock:icon=./favicon.ico
fi

${JAVA_CMD} $xdock -DsocksProxyHost=10.75.110.1 -DsocksProxyPort=1088 -Dapl.runtime.mode=desktop -Dapl.enablePeerUPnP=false -jar ${MAIN_JAR}


