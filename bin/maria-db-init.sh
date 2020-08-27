#!/bin/bash


echo 'basedir:'$1
echo 'conf:'$2

#full directory name of the script location no matter where it is being called from.
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
echo 'Script location: ' ${DIR}


cd $1/bin
#1) Install system db tables.
#basedir есть внутри конфига, но от туда не читается. todo Посмотреть альтернативу: mysql_secure_installation
./mysql_install_db --defaults-file=$2 --basedir=$1 --verbose

#--insecure - don't work
#--initialize-insecure - don't work
#--password=root - don't work
#--authentication-method=normal - don't work
#--extra-sql-file=DIR/maria-initial.sql - can't change/crete user password there.

#2)Run server.
./mariadbd  --defaults-file=$2 --verbose &

#Waiting for run server.
sleep 4

#3)Create password (first time)
#without sudo 'error: 'Access denied for user 'root'@'localhost'''
./mysqladmin --defaults-file=$2 -u root password 'root'

#4)Connect to the server.
#./mariadb --defaults-file=$2 --socket=$3 -u root -p


