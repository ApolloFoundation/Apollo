#!/bin/bash

# (C) 2019 Apollo Foundation 

# Downloads Apollo blockchain database and extracts it to user's home location



DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"



 . ${DIR}/apl-common.sh 



${DIR}/apl-stop.sh



DB_LOCATION=~/.apl-blockchain/apl-blockchain-db

rm -rf $DB_LOCATION

mkdir $DB_LOCATION

DB_FILE=b5d7b6.jar

DB_URL="https://s3.amazonaws.com/updates.apollowallet.org/database/${DB_FILE}"

echo "Exctractiong into ${DB_LOCATION}"

cd $DB_LOCATION

curl --output $DB_FILE $DB_URL

${JAR_CMD} -xvf $DB_FILE

rm -rf META-INF

rm -f $DB_FILE
