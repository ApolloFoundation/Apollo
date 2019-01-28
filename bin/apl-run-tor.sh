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

if [[ "$unamestr" == 'Linux' ]]; then
    cd tor
    ./tor &
    cd ..
fi

${JAVA} -DsocksProxyHost=localhost -DsocksProxyPort=9050 -Dapl.runtime.mode=desktop -jar Apollo.jar


