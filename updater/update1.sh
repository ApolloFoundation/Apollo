#!/bin/bash
# This script invoked by update.sh script to detach script process from Java main app
# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode

APOLLO_JAR="apl-exec"

APLCMDLINE=$(cat ~/.apl-blockchain/apl.cmdline 2>/dev/null)

unamestr=`uname`


function notify
{
    if [[ "$unamestr" == 'Darwin' ]]; then
	osascript -e "display notification \"$1\" with title \"Apollo\""
    else
	echo $1
    fi
}

function getNetwork()
{
    NETWORK=0
    if [ $(cat ~/.apl-blockchain/apl.cmdline | grep "\-n" | wc -l) -eq 1 ]
    then
        NETWORK=$(cat ~/.apl-blockchain/apl.cmdline | grep -oE "(\-n|\-\-net)\s{1,}[0-9]{1}" | cut -f2 -d' ')
    else
        NETWORK=0
    fi


}

function getConfigPath()
{
    getNetwork
    if  [ ${NETWORK} -eq 0 ]
    then
	CONFIGDIR=conf
    else
	CONFIGDIR=conf-tn${NETWORK}
    fi

    if [ $3 == "true" ]
    then
	CONFIGDIR=~/.apl-blockchain/${CONFIGDIR}
    else
	CONFIGDIR=$1/${CONFIGDIR}
    fi
}

function isSharding()
{
    NOSHARD=false
    if [ $(cat ~/.apl-blockchain/apl.cmdline |  grep -oE "\--no-shards-create\s{1,}true" | wc -l) -eq 1 ]
    then
	NOSHARD=true
    else
	if [ $(cat ${CONFIGDIR}/apl-blockchain.properties | grep apl.noshardcreate | grep -v "#" | wc -l ) -eq 1 ]
	then
    	    NOSHARD=$(cat ${CONFIGDIR}/apl-blockchain.properties | grep apl.noshardcreate | grep -v "#" | cut -f2 -d'=')
	else
	    NOSHARD=false
        fi
    fi
}


VERSION=$(head -n1 ${2}/apollo-blockchain/VERSION)

if  [[ -d "${1}" ]] && [[ -d "${2}" ]] && [[ -n "${3}" ]]
then

    notify "Starting Apollo Updater"
    notify "Stopping Apollo Wallet"

    NEXT_WAIT_TIME=0

# For backwards compatibility
    if [ ! -f VERSION ]; then
#	echo "Runnung from updater directory...." >>/tmp/updater_ne_tuda
	cd ..
    fi

    kill $(ps -ef | grep apl-de | awk '{ print $2 }')
    $1/bin/apl-stop.sh
    until [ $(ps aux | grep ${APOLLO_JAR} | grep -v grep | wc -l) -eq 0 ] || [ $NEXT_WAIT_TIME -eq 10 ]; do
	NEXT_WAIT_TIME=`expr $NEXT_WAIT_TIME '+' 1`
	sleep $NEXT_WAIT_TIME
	notify "Waiting more time to stop Apollo Wallet..."
    done

# it is always good idea to backup everything before removing
#NOW=`date +%Y-%m-%dT%H:%m:%S`
#BKP_NAME=${1}/../ApolloWallet-BKP-${NOW}.tar.gz
#tar -czf ${BKP_NAME} ${1}

# we sould remove "conf" dir because default configs are in resources now
# and user's configs are in ~/.apl_blockchain
    if [ -f $1/conf/apl.properties ]; then
	rm -rf $1/conf/apl-default.properties
        rm -rf $1/conf/testnet.properties
	rm -rf $1/conf/updater.properties
	cp $1/conf/apl.properties $1/conf/apl-blockchain.properties
	cp $1/conf/apl.properties $1/conf/apl.properties.backup
	cat $1/conf/apl.properties | grep -v "#" | grep apl.dbDir= | sed s/dbDir/customDbDir/ >> $1/conf/apl-blockchain.properties
        rm $1/conf/apl.properties
    fi

    if [ -f $1/conf/chains.json ]; then
	cp -f $1/conf/chains.json $1/conf/chains.json.backup
	rm -f $1/conf/chains.json
    fi


    notify "Removing old version..."
    notify "Removing garbage..."
    rm -rfv $1/bin
    rm -rfv $1/guilib
    rm -rfv $1/html-stub
    rm -rfv $1/lib
    rm -rfv $1/sbin
    rm -rfv $1/updater
    rm -rfv $1/webui
    rm -rfv $1/3RD-PARTY-LICENSES.txt
    rm -rfv $1/VERSION*
    rm -rfv $1/LICENSE*
    rm -rfv $1/*jar
    rm -rfv $1/META-INF
    rm -rfv $1/update*

    cd $1/..
    rm -rfv apollo-web-ui
    rm -rfv apollo-tools

    chmod 755 $1/../apollo-blockchain/bin/*.sh

    cd $1/..
    chmod 755 $1/../apollo-desktop/bin/*.sh

    notify "Removing old version..."

    notify "Moving extra files..."
#    cp -Rfv $1/* $1/..
    cd $1/..
#    rm -rfv $1
    cp -Rfv $2/* .

#    cd $1/
#    cp -vRa conf* apollo-blockchain
#    cp -vRa conf/* apollo-blockchain/conf
#    ./replace_dbdir.sh

#    notify "Creating symlinks..."
#    ln -s apollo-blockchain/bin bin


    chmod 755 apollo-blockchain/bin/*.sh
    chmod 755 apollo-desktop/bin/*.sh

    if [[ "$unamestr" == 'Darwin' ]]; then

	echo "#!/bin/bash" > ApolloWallet.app/Contents/MacOS/apl
	echo "" >> ApolloWallet.app/Contents/MacOS/apl
	echo "cd \"$1/../apollo-desktop/bin\"" >> ApolloWallet.app/Contents/MacOS/apl
	echo "./apl-start-desktop.sh" >> ApolloWallet.app/Contents/MacOS/apl

	echo "#!/bin/bash" > ApolloWallet+Tor.app/Contents/MacOS/apl
	echo "" >> ApolloWallet+Tor.app/Contents/MacOS/apl
	echo "cd \"$1/../ApolloWallet+TOR.app/tor/bin\"" >> ApolloWallet+Tor.app/Contents/MacOS/apl
        echo "./tor &" >> ApolloWallet+Tor.app/Contents/MacOS/apl
	echo "cd \"$1/../apollo-desktop/bin\"" >> ApolloWallet+Tor.app/Contents/MacOS/apl
	echo "./apl-start-desktop.sh tor" >> ApolloWallet+Tor.app/Contents/MacOS/apl

	echo "#!/bin/bash" > ApolloWallet+Secure\ Transport.app/Contents/MacOS/apl
	echo "" > ApolloWallet+Secure\ Transport.app/Contents/MacOS/apl
	echo "cd \"$1/../ApolloWallet+Secure Transport.app/secureTransport/\"" >> ApolloWallet+Secure\ Transport.app/Contents/MacOS/apl
        echo "osascript -e 'do shell script \"./runClient.sh\" with administrator privileges' &" >> ApolloWallet+Secure\ Transport.app/Contents/MacOS/apl
	echo "cd \"$1/../apollo-desktop/bin\"" >> ApolloWallet+Secure\ Transport.app/Contents/MacOS/apl
	echo "./apl-start-desktop.sh secure-transport" >> ApolloWallet+Secure\ Transport.app/Contents/MacOS/apl

	chmod 755 "ApolloWallet+Secure Transport.app/Contents/MacOS/apl"
	chmod 755 "ApolloWallet+Secure Transport.app/secureTransport/securenodexchg"
	chmod 755 "ApolloWallet+Secure Transport.app/secureTransport/runClient.sh"
	chmod 755 "ApolloWallet+Tor.app/Contents/MacOS/apl"
	chmod 755 "ApolloWallet+Tor.app/tor/bin/tor"
	chmod 755 "ApolloWallet.app/Contents/MacOS/apl"
#	rm -rf "$1/ApolloWallet+Secure Transport.app"
#	rm -rf "$1/ApolloWallet+Tor.app"
#	rm -rf "$1/ApolloWallet.app"

    fi

    if [[ "$unamestr" == 'Linux' ]]; then

	chmod 755 tor/tor
	chmod 755 secureTransport/securenodexchg
	chmod 755 secureTransport/runClient.sh
    fi

#    rm -rf apollo-wallet-deps-${VERSION}.tar.gz
#    rm -rf apollo-wallet-deps-*
    echo Version = ${VERSION}
#    notify "Downloading deps...."
# Commented downloading deps
#    wget https://s3.amazonaws.com/updates.apollowallet.org/libs/apollo-wallet-deps-${VERSION}.tar.gz || curl --retry 100  https://s3.amazonaws.com/updates.apollowallet.org/libs/apollo-wallet-deps-${VERSION}.tar.gz -o apollo-wallet-deps-${VERSION}.tar.gz
#    tar -zxvf apollo-wallet-deps-${VERSION}.tar.gz
#    cp apollo-wallet-deps-${VERSION}/* $1/lib

#    rm -rf apollo-wallet-deps-${VERSION}*

# Install JRE
#    notify "Installing Java Runtime..."
#    bash ./update2.sh $1

#determine, if shrding was performed or not





# Download db with shards
    getNetwork
    getConfigPath $1 $2 $3
    isSharding

    case ${NETWORK} in
	0)
	    NETID=b5d7b6
	    ;;
	1)
	    NETID=a2e9b9
	    ;;
	2)
	    NETID=2f2b61
	    ;;
	*)
	    NETID=b5d7b6

    esac
    notify "Downloading database"
# TODO: ! refactor and ncomment that block
    if [ "$#" -eq 3 ]
    then
	if [ ${NOSHARD} == false ]
	then
	    bash ./update3.sh $1 $2 $3 true ${NETID}
	fi
    else
    
	bash ./update3.sh $1 $2 $3 $4 $5
    fi


#    if [[ -d conf ]]; then
#	mv -fv conf apollo-blockchain
#    fi
    APLCMDLINE=$(echo ${APLCMDLINE} | sed s/shards/shard/g)
    if [ $3 == true ]
    then
        notify "Starting desktop application..."
        cd apollo-desktop/bin
        nohup ./apl-start-desktop.sh ${APLCMDLINE} 2>&1 >/dev/null
    else
        notify "Starting command line application..."
        cd apollo-blockchain/bin
        ./apl-start.sh ${APLCMDLINE}
    fi

else
    echo Invalid input parameters $1,$2,$3
fi
