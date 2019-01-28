#!/bin/bash
# This script is used to install jre on client machines, because update package with jre is too large
unamestr=`uname`

function notify
{
    if [[ "$unamestr" == 'Darwin' ]]; then
    osascript -e "display notification \"$1\" with title \"Apollo\""
    else
    echo $1
    fi
}

if  [ -d $1 ]
then

    VERSION=$(cat VERSION)

#    notify "Preparing..."
    
    rm -rf $1/jre
    rm -rf $1/lib

#    notify "Updating Java runtime..."
        
    if [[ "$unamestr" == 'Linux' ]]; then
	curl -o $1/jre.tar.gz https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz
	tar -C $1 -zxvf $1/jre.tar.gz 
	mv $1/jdk-11.0.1 $1/jre
    fi
    
    if [[ "$unamestr" == 'Darwin' ]]; then
	curl -o $1/jre.tar.gz https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_osx-x64_bin.tar.gz
	tar -C $1 -zxvf jre.tar.gz
	mv $1/jdk-11.0.1/Contents/Home $1/jre
	rm -rf $1/jdk-11.0.1
    fi
    rm -rf $1/jdk-11.0.1
    
    rm $1/jre.tar.gz

    if [[ "$unamestr" == 'Darwin' ]]; then
	chmod 755 $1/jre/bin/* $1/jre/lib/lib*
	chmod 755 $1/jre/lib/jspawnhelper $1/jre/lib/jli/* $1/jre/lib/lib*
    elif [[ "$unamestr" == 'Linux' ]]; then
	chmod 755 $1/jre/bin/*
    fi
    
#    notify "Updating libraries..."
    
    curl -o $1/libs.tar.gz https://s3.amazonaws.com/updates.apollowallet.org/libs/ApolloWallet-$VERSION-libs.tar.gz
    tar -C $1 -zxvf $1/libs.tar.gz
    mv $1/ApolloWallet-$VERSION-libs $1/lib

else
    echo Invalid input parameters $1
fi
