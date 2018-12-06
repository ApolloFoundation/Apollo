#!/bin/sh
SCRIPT=`realpath -s $0`
SCRIPT_DIR=`dirname $SCRIPT`
TOP_DIR=`dirname $SCRIPT_DIR`
LIB_DIR=`realpath -s ${TOP_DIR}/lib`

if [ -x ${TOP_DIR}/jre/bin/java ]; then
    JAVA=${TOP_DIR}/jre/bin/java
else
    JAVA=java
fi
${JAVA}  -Dapl.runtime.mode=desktop -jar ${TOP_DIR}/Apollo.jar
