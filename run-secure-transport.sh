#!/bin/bash

# WARNING: java still bypasses the tor proxy when sending DNS queries and
# this can reveal the fact that you are running Apl, however blocks and
# transactions will be sent over tor only. Requires a tor proxy running
# at localhost:9050. Set apl.shareMyAddress=false when using tor.

if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi

unamestr=`uname`
if [[ "$unamestr" == 'Linux' ]]; then

    cd secureTransport
    sudo ./runClient.sh 
    cd ..
fi



${JAVA} -DsocksProxyHost=10.75.110.1 -DsocksProxyPort=1088 -Dapl.runtime.mode=desktop -Dapl.enablePeerUPnP=false -jar Apollo.jar


