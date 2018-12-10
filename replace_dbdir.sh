#!/bin/bash

cat conf/apl.properties | sed 's,./apl_db/apl,./apl_db,' >> conf/apl.properties_
cat conf/apl.properties_ | sed 's,./apl_test_db/apl,./apl_test_db,' >> conf/apl.properties__
cp -f conf/apl.properties__ conf/apl.properties
rm -rf conf/apl.properties_ conf/apl.properties__
