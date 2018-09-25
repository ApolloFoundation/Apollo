#!/bin/sh
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
${JAVA}  -Dapl.runtime.mode=desktop -cp addons/classes:addons/lib/* -jar Apollo.jar
