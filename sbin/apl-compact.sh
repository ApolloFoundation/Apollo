#!/bin/sh
# (C) 2019 Apollo Foundation 

SCRIPT=`realpath -s $0`
DIR=`dirname $SCRIPT`
 . ${DIR}/../bin/apl-common.sh

#"***********************************************************************"
#"* This shell script will compact and reorganize the apl-blockchain db *"
#"* This process can take a long time.  Do not interrupt the script     *"
#"* or shutdown the computer until it finishes.                         *"
#"*                                                                     *"
#"* To compact the database used while in a user mode, i.e. located     *"
#"* under ~/.apl-blockchain/apl-blockchain-db/chainId , invoke this     *"
#"* script as: ./apl-compact.sh -Dapl.runtime.mode=user                 *"
#echo "*******************************************************************"

${JAVA_CMD}  -jar ${APL_TOOLS_JAR} compactdb
# $@ 
exit $?
