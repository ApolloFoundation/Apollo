#!/bin/sh
if [ -x jre/bin/java ]; then
    JAVA=./jre/bin/java
else
    JAVA=java
fi
${JAVA} -cp target/classes:target/lib/*:conf:addons/classes:addons/lib/* -Dapl.runtime.mode=desktop -Dapl.runtime.dirProvider=com.apollocurrency.aplwallet.apl.env.DefaultDirProvider com.apollocurrency.aplwallet.apl.Apl
