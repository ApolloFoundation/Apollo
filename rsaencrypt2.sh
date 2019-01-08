#!/usr/bin/env bash

for i in $1 $2 $3
do
    java -cp "target/classes:target/lib/*" com.apollocurrency.aplwallet.apl.tools.RSAEncryption false $i ../rost-privkey4.pem
    echo
done
