#!/bin/sh
# Define versions
if [ -z "${1}" ] ; then
    NEW_VERSION=1.47.4
else
    NEW_VERSION=$1
fi
VF="VERSION"
# set versions in parent pom and in all childs
echo "New version is: $NEW_VERSION"
echo $NEW_VERSION > $VF
echo "Changing Maven's POM files"
./mvnw versions:set -DnewVersion=${NEW_VERSION}
cd apl-bom || exit
./mvnw versions:set -DnewVersion=${NEW_VERSION}
cd ..

# TODO: Prettify this block.
# set verions in Constants.java (application hardcoded version)
CONST_PATH=apl-utils/src/main/java/com/apollocurrency/aplwallet/apl/util/Constants.java
echo "Changing Constants in $CONST_PATH"
VER_STR="VERSION"
sed -i -e "s/\ VERSION.*/ VERSION = new Version\(\"$NEW_VERSION\"\);/g" ${CONST_PATH}

PKG_PATH=apl-exec/packaging/pkg-apollo-blockchain.json
echo "Changing pkg-apollo-blockchain.json"
sed -i -e "s/\ \"version\".*/ \"version\": \"$NEW_VERSION\",/g" ${PKG_PATH}

README_PATH=README.md
echo "Changing README.md..."

sed -i -E "s/___apollo-blockchain-[0-9]{1,}\.[0-9]{1,}\.[0-9]{1,}/___apollo-blockchain-$NEW_VERSION/g" ${README_PATH}
