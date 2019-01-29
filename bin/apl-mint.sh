#!/bin/sh
# (C) 2019 Apollo Foundation 
# Starts Apollo blockchain in foreground
SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`

 . ${DIR}/apl-common.sh 

echo "***********************************************************************"
echo "* This shell script will start minting worker for mintable currencies *"
echo "* Take a look at 'Mint' section in apl.properties for detailed config *"
echo "***********************************************************************"

${JAVA_CMD} -jar ${APL_LIB_DIR}/apl-tools-${APL_VERSION}.jar mint

