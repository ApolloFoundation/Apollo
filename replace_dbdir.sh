#!/bin/bash

cat conf/apl-blockchain.properties | sed 's,./apl_db/apl,./apl_db,' >> conf/apl-blockchain.properties_
cat conf/apl-blockchain.properties_ | sed 's,./apl_test_db/apl,./apl_test_db,' >> conf/apl-blockchain.properties__
cp -f conf/apl-blockchain.properties__ conf/apl-blockchain.properties
rm -rf conf/apl-blockchain.properties_ conf/apl-blockchain.properties__
