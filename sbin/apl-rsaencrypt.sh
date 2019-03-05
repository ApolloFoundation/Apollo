#!/bin/bash
echo "***********************************************************************"
echo "* This shell script will encrypt data using RSA private key           *"
echo "* Double encryption supported. Program will launch in interactive mode*"
echo "* by default. You can pass parameters to the executable class to      *"
echo "* disable interactive mode. Use case - encryption of updater urls.    *"
echo "* Parameters (order is important)                                     *"
echo "* a) isHexadecimal - boolean flag that indicates that you want to pass*"
echo "*    to encryption not the ordinary string, but hexadecimal           *"
echo "* b) hexadecimal string of message bytes or just message depending    *"
echo "*    on option isHexadecimal                                          *"
echo "* c) private key path (absolute). Encrypted keys are not supported    *"
echo "* TODO fix app  launch                                                *"
echo "***********************************************************************"
SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`
 . ${DIR}/../bin/apl-common.sh

${JAVA_CMD}  -jar ${APL_TOOLS_JAR} updaterurl --encrypt --in $1 --out $2
exit $?
