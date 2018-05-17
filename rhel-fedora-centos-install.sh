#!/bin/bash

java -version

if [ $? -eq 127 ]
then
    wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u172-b11/a58eab1ec242421181065cdc37240b08/jdk-8u172-linux-x64.rpm"
    rpm -ivh jdk-8u172-linux-x64.rpm
    rm jdk-8u172-linux-x64.rpm
fi

echo "Enter address for this node:"

read ipaddress

cat conf/apl-default.properties | sed s/apl.myAddress\=/apl.myAddress\=$ipaddress/ > conf/apl.properties

./compile.sh

mkdir logs

echo Installation completed successfully!
echo Use run.sh to start desktop wallet
echo Or start.sh to start web wallet
echo You can access web wallet with Your browser by the URL http://localhost:7876/
