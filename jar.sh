#!/bin/sh
APPLICATION="Apollo"
java -cp classes com.apollocurrency.aplwallet.apl.tools.ManifestGenerator
/bin/rm -f ${APPLICATION}.jar
jar cfm ${APPLICATION}.jar resource/apl.manifest.mf -C classes . || exit 1
/bin/rm -f ${APPLICATION}service.jar
jar cfm ${APPLICATION}service.jar resource/aplservice.manifest.mf -C classes . || exit 1

echo "jar files generated successfully"