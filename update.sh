#!/bin/sh

# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode
if  [[ -d $1 && -d $2 && -n $3 ]]
then
    echo Starting Platform Dependent Updater
    echo Stopping wallet.... 
    NEXT_WAIT_TIME=0
    
    until [ $(ps aux | grep Apollo.jar | wc -l) -eq 0 ] || [ $NEXT_WAIT_TIME -eq 10 ]; do
	sleep $(( NEXT_WAIT_TIME++ ))
	echo "Waiting more time to stop wallet..."
    done

    echo Copy update files
    cp -TRa $2 $1

    if [ $3 == true ]
    then
        echo Start desktop application
        cd $1 && exec sh start-desktop.sh
    else
        echo Start command line application
        cd $1 && exec sh start.sh
    fi

else
    echo Invalid input parameters $1,$2,$3
fi