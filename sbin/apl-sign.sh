#!/bin/sh
java -cp "target/classes:target/lib/*:conf" com.apollocurrency.aplwallet.apl.tools.SignTransactionJSON $@
exit $?
