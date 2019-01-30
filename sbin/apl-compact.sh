#!/bin/sh
echo "***********************************************************************"
echo "* This shell script will compact and reorganize the apl-blockchain db *"
echo "* This process can take a long time.  Do not interrupt the script     *"
echo "* or shutdown the computer until it finishes.                         *"
echo "*                                                                     *"
echo "* To compact the database used while in a user mode, i.e. located     *"
echo "* under ~/.apl-blockchain/apl-blockchain-db/chainId , invoke this     *"
echo "* script as: ./compact.sh -Dapl.runtime.mode=user                     *"
echo "***********************************************************************"

java -Xmx1024m -cp "classes:lib/*:conf" $@ com.apollocurrency.aplwallet.apl.tools.CompactDatabase -Dapl.runtime.mode=user
exit $?
