#!/bin/bash
#"***********************************************************************"
#"* This shell script will decrypt data encrypted by RSA private key    *"
#"* Double decryption supported. Program will launch in interactive mode*"
#"* by default. You can pass parameters to the executable class to      *"
#"* disable interactive mode. Use case - decryption of updater urls.    *"
#"* Parameters (order is important)                                     *"
#"* a) certificate path (absolute)                                      *"
#"* b) hexadecimal string of encrypted message bytes                    *"
#"* c) boolean flag that indicates that you want to convert decrypted   *"
#"* bytes real to real UTF-8 string                                     *"
#"* TODO fix app  launch                                                *"
#"***********************************************************************"
SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`
 . ${DIR}/../bin/apl-common.sh

${JAVA_CMD}  -jar ${APL_TOOLS_JAR} updaterurl --decrypt --in $1 --out $2
exit $?
