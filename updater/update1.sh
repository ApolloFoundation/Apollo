#!/bin/bash
# This script invoked by update.sh script to detach script process from Java main app
# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode

APOLLO_JAR="Apollo.jar"

unamestr=`uname`

function notify
{
    if [[ "$unamestr" == 'Darwin' ]]; then
	osascript -e "display notification \"$1\" with title \"Apollo\""
    else
	echo $1
    fi
}


if  [[ -d "${1}" ]] && [[ -d "${2}" ]] && [[ -n "${3}" ]]
then
    
    notify "Starting Apollo Updater"
    notify "Stopping Apollo Wallet"

    NEXT_WAIT_TIME=0
    
    until [ $(ps aux | grep ${APOLLO_JAR} | grep -v grep | wc -l) -eq 0 ] || [ $NEXT_WAIT_TIME -eq 10 ]; do
	NEXT_WAIT_TIME=`expr $NEXT_WAIT_TIME '+' 1`
	sleep $NEXT_WAIT_TIME
	notify "Waiting more time to stop Apollo Wallet..."
    done
    
# it is always good idea to backup everything before removing
NOW=`date +%Y-%m-%dT%H:%m:%S`
BKP_NAME=${1}/../ApolloWallet-BKP-${NOW}.tar.gz 
tar -czf ${BKP_NAME} ${1}

# we sould remove "conf" dir because default configs are in resources now
# and user's configs are in ~/.apl_blockchain
    rm -rf $1/conf
#may be we have to remove garbage    
    rm -f $1/*.sh
    rm -f $1/*.bat
    rm -f $1/*.vbs
    rm -rf $1/META-INF
    rm -rf $1/html
    rm -f $1/Apollo.jar
    
    notify "Copying update files...."
    cp -vRa $2/* $1
    
    
    if [[ "$unamestr" == 'Darwin' ]]; then
	mv "$1/ApolloWallet+Secure Transport.app" $1/../
	mv "$1/ApolloWallet+Tor.app" $1/../
	chmod 755 "$1/../ApolloWallet+Secure Transport.app/Contents/MacOS/apl"
	chmod 755 "$1/../ApolloWallet+Secure Transport.app/secureTransport/securenodexchg"
	chmod 755 "$1/../ApolloWallet+Secure Transport.app/secureTransport/*.sh"
	chmod 755 "$1/../ApolloWallet+Tor.app/Contents/MacOS/apl"
	chmod 755 "$1/../ApolloWallet+Tor.app/tor/bin/tor"
    fi

    if [[ "$unamestr" == 'Linux' ]]; then
	chmod 755 $1/tor/tor
	chmod 755 $1/secureTransport/securenodexchg
	chmod 755 $1/secureTransport/runClient.sh
    fi

# Install JRE
#    notify "Installing Java Runtime..."
#    bash ./update2.sh $1

    cd $1 
    chmod 755 *.sh

    cd $1 
    chmod 755 *.sh
    
#    ./replace_dbdir.sh
    
    if [ $3 == true ]
    then
        notify "Starting desktop application..."
        nohup ./bin/apl-run-desktop.sh 2>&1 >/dev/null
    else
        notify "Starting command line application..."
        ./bin/apl-start.sh
    fi

else
    echo Invalid input parameters $1,$2,$3
fi
