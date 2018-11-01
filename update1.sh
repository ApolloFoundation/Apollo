#!/bin/bash

# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode
if  [ -d $1 ] && [ -d $2 ] && [ -n $3 ]
then
    echo Starting Platform Dependent Updater
    echo Stopping wallet.... 
    NEXT_WAIT_TIME=0
    
    until [ $(ps aux | grep Apollo.jar | grep -v grep | wc -l) -eq 0 ] || [ $NEXT_WAIT_TIME -eq 10 ]; do
	NEXT_WAIT_TIME=`expr $NEXT_WAIT_TIME '+' 1`
	sleep $NEXT_WAIT_TIME
	echo "Waiting more time to stop wallet..."
    done

    echo Copy update files
    cp -vRa $2/* $1

    cd $1 
    chmod 755 *.sh

    cd $1 
    chmod 755 *.sh

    if [ $3 == true ]
    then
        echo Start desktop application
        ./start-desktop.sh
    else
        echo Start command line application
        ./start.sh
    fi

else
    echo Invalid input parameters $1,$2,$3
fi
