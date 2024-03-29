#!/bin/bash
# (C) 2019 Apollo Foundation
# Starts Apollo blockchain in foreground with TOR proxy
# Required for Linux/MacOs installers.
TOR_DIST_DIR=tor

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
#"

 . "${DIR}"/apl-common.sh

unamestr=`uname`

# WARNING: java still bypasses the tor proxy when sending DNS queries and
# this can reveal the fact that you are running Apl, however blocks and
# transactions will be sent over tor only. Requires a tor proxy running
# at localhost:9050. Set apl.shareMyAddress=false when using tor.

TOR_DIR="${APL_TOP_DIR}"/../"${TOR_DIST_DIR}"
echo Tor dir  = ${TOR_DIR}
if [ -x "${TOR_DIR}" ];  then
    TOR_CMD="${TOR_DIR}/tor"
else
  if [[ -n $(type -p tor) ]]
  then
    TOR_CMD="./tor &"
  else
    TOR_CMD=""
  fi
fi

if [[ ${unamestr} = "Darwin" ]]
then
    "${JAVA_CMD}" -DsocksProxyHost=localhost -DsocksProxyPort=9050  -jar "${MAIN_JAR}" &
    exit 0
fi

if [ -z "${TOR_CMD}" ]; then
    echo " Tor not found. You can install tor using package manager or download"
    echo " Tor Browser from official site https://www.torproject.org and install"
    echo " it in ${APL_TOP_DIR}"
else
    echo "Starting tor"
    cd "${TOR_DIR}"
    "$TOR_CMD" &
    if [ "$?" == 0 ] ; then
      echo "Starting Apollo"
      cd "${DIR}"
      "${JAVA_CMD}" -DsocksProxyHost=localhost -DsocksProxyPort=9050  -jar "${MAIN_JAR}"
    else
      echo
      echo "ERROR! Tor execution failed. Please configure tor properly."
      echo "Read docs at official site https://www.torproject.org"
      exit 1
    fi
fi

