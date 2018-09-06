#!/bin/sh
CP="target/lib/*;target/classes"
SP=src/java/

/bin/rm -rf html/doc/*

javadoc -quiet -sourcepath $SP -classpath "$CP" -protected -splitindex -subpackages apl -d html/doc/
