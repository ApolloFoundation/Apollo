#!/bin/sh
java -cp "classes:lib/*:conf" apl.tools.SignTransactionJSON $@
exit $?
