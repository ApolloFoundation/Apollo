#!/bin/bash
# This script invoked by update.sh script to detach script process from Java main app
# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode

unamestr=`uname`

function notify
{
    if [[ "$unamestr" == 'Darwin' ]]; then
	osascript -e "display notification \"$1\" with title \"Apollo\""
    else
	echo $1
    fi
}


if  [ -d $1 ] && [ -d $2 ] && [ -n $3 ]
then
    
    notify "Starting Apollo Updater"
    notify "Stopping Apollo Wallet"

    NEXT_WAIT_TIME=0
    
    until [ $(ps aux | grep Apollo.jar | grep -v grep | wc -l) -eq 0 ] || [ $NEXT_WAIT_TIME -eq 10 ]; do
	NEXT_WAIT_TIME=`expr $NEXT_WAIT_TIME '+' 1`
	sleep $NEXT_WAIT_TIME
	notify "Waiting more time to stop Apollo Wallet..."
    done
    

    
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

    if [[ "$unamestr" == 'Darwin' ]]; then
	chmod 755 $1/jre/bin/* $1/jre/lib/lib*
	chmod 755 $1/jre/lib/jspawnhelper $1/jre/lib/jli/* $1/jre/lib/lib*
    elif [[ "$unamestr" == 'Linux' ]]; then
	chmod 755 $1/jre/bin/*
    fi



    cd $1 
    chmod 755 *.sh

    cd $1 
    chmod 755 *.sh
    
#    ./replace_dbdir.sh
    
    if [ $3 == true ]
    then
        notify "Starting desktop application..."
        ./start-desktop.sh
    else
        notify "Starting command line application..."
        ./start.sh
    fi

else
    echo Invalid input parameters $1,$2,$3
fi
