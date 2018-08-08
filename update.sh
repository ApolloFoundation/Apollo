#!/bin/sh

# first parameter is a current directory, where wallet is executing now (directory, which we should update)
# second parameter is a update directory which contains unpacked jar for update
# third parameter is a boolean flag, which indicates desktop mode
if  [[ -d $1 && -d $2 && -n $3 ]]
then
    echo Starting Platform Dependent Updater
    echo Waiting 3 sec
    sleep 3
    CURRENT_CONF_FILE=$1'/conf/apl.properties'
    UPDATE_CONF_FILE=$2'/conf/apl.properties'
    if [[ -f $CURRENT_CONF_FILE && -f $UPDATE_CONF_FILE ]]
    then
        echo Copy config file
        cp -f $CURRENT_CONF_FILE $UPDATE_CONF_FILE
        echo Copy update files
        cp -TRa $2 $1

        if [ $3 == true ]
        then
            echo Start desktop application
            $1/run-desktop.sh
        else
            echo Start command line application
            $1/run.sh
        fi
    else
        echo Config files: $CURRENT_CONF_FILE or $UPDATE_CONF_FILE do not exist!
    fi
else
    echo Invalid input parameters $1,$2,$3
fi