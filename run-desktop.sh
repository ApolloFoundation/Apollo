#!/bin/sh
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
${JAVA} -cp classes:lib/*:conf:addons/classes:addons/lib/* -Dapl.runtime.mode=desktop -Dapl.runtime.dirProvider=apl.env.DefaultDirProvider apl.Apl
