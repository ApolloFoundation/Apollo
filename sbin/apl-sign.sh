#!/bin/sh
echo "*********************************************************************"
echo "* Sign transaction offline and manually                             *"
echo "* This batch file will sign transaction json stored in file and     *"
echo "* write signed transaction to output file. Works in combined mode   *"
echo "* Read secretPhrase(standard wallet) or secretKeyBytes(vault wallet)*"
echo "* from console                                                      *"
echo "* Cmd parameters (order is important)                               *"
echo "* a) Unsigned transaction file path - mandatory                     *"
echo "* a) Signed transaction file path - optional                        *"
echo "* TODO fix app launch                                               *"
echo "*********************************************************************"

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`
 . ${DIR}/../bin/apl-common.sh

${JAVA_CMD}  -jar ${APL_TOOLS_JAR} signtx -json
exit $?
