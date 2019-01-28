#!/bin/sh
echo "***********************************************************************"
echo "* This shell script will start minting worker for mintable currencies *"
echo "* Take a look at 'Mint' section in apl.properties for detailed config *"
echo "***********************************************************************"
java -cp target/classes:target/lib/*:conf com.apollocurrency.aplwallet.apl.mint.MintWorker
