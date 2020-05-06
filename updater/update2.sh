#!/bin/bash
# This script is used to install jre on client machines, because update package with jre is too large
unamestr=`uname`
#
OPENJDK_URL_Linux="https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz"
OPENJDK_URL_Darwin="https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_osx-x64_bin.tar.gz"
OPENJDK_DIR="jdk-11.0.2"
#
APOLLO_LIBS_URL_PREFIX="https://s3.amazonaws.com/updates.apollowallet.org/libs/"
APOLLO_LIBS_NAME_PREFIX="apollo-wallet-deps"
function notify
{
    if [[ "$unamestr" == 'Darwin' ]]; then
    osascript -e "display notification \"$1\" with title \"Apollo\""
    else
    echo $1
    fi
}

if  [[ -d $1 ]]
then

    VERSION=$(cat VERSION)
    
    APOLLO_LIBS_DIR=${APOLLO_LIBS_NAME_PREFIX}-${VERSION}
    APOLLO_LIBS_FILE=${APOLLO_LIBS_DIR}.tar.gz
    APOLLO_LIBS_URL=${APOLLO_LIBS_URL_PREFIX}${APOLLO_LIBS_FILE}
    APOLLO_LIBS_SHA256=${APOLLO_LIBS_FILE}.sha256
    APOLLO_LIBS_SHA256_URL=${APOLLO_LIBS_URL_PREFIX}${APOLLO_LIBS_SHA256}
    
#    notify "Preparing..."
    
    rm -rf $1/jre
    rm -rf $1/lib

#    notify "Updating Java runtime..."
        
    if [[ "$unamestr" == 'Linux' ]]; then
	wget $OPENJDK_URL_Linux || curl -o $1/jre.tar.gz $OPENJDK_URL_Linux
	tar -C $1 -zxvf $1/jre.tar.gz 
	mv $1/$OPENJDK_DIR $1/jre
    fi
    
    if [[ "$unamestr" == 'Darwin' ]]; then
	wget $OPENJDK_URL_Darwin || curl -o $1/jre.tar.gz $OPENJDK_URL_Darwin
	tar -C $1 -zxvf jre.tar.gz
	mv $1/$OPENJDK_DIR/Contents/Home $1/jre
	rm -rf $1/$OPENJDK_DIR
    fi
    rm -rf $1/$OPENJDK_DIR
    
    rm -f $1/jre.tar.gz

    if [[ "$unamestr" == 'Darwin' ]]; then
	chmod 755 $1/jre/bin/* $1/jre/lib/lib*
	chmod 755 $1/jre/lib/jspawnhelper $1/jre/lib/jli/* $1/jre/lib/lib*
    elif [[ "$unamestr" == 'Linux' ]]; then
	chmod 755 $1/jre/bin/*
    fi
    
#    notify "Updating libraries..."
    
#    curl -o $1/libs.tar.gz $APOLLO_LIBS_URL
#    curl -o $1/libs.tar.gz.sha256 $APOLLO_LIBS_SHA256_URL
#    tar -C $1 -zxvf $1/libs.tar.gz
    
#    mv $1/${APOLLO_LIBS_DIR} $1/lib
#    rm -f $1/${APOLLO_LIBS_FILE}
#    rm -f $1/${APOLLO_LIBS_FILE}.sha256
else
    echo Invalid input parameters $1
    exit 2
fi
