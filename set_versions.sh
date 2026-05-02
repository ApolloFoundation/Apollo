#!/bin/sh
# Define versions
if [ -z "${1}" ]; then
    NEW_VERSION=1.48.0
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

# set versions in Constants.java (application hardcoded version)
CONST_PATH="apl-utils/src/main/java/com/apollocurrency/aplwallet/apl/util/Constants.java"
PKG_PATH="apl-exec/packaging/pkg-apollo-blockchain.json"

echo "Updating Constants.java: $CONST_PATH"
sed -i -E \
  "s/(VERSION[[:space:]]*=[[:space:]]*new Version\().*(\);)/\1\"$NEW_VERSION\"\2/" \
  "$CONST_PATH"

echo "Updating package file: $PKG_PATH"
sed -i -E \
  "s/(\"version\"[[:space:]]*:[[:space:]]*\").*(\",)/\1$NEW_VERSION\2/" \
  "$PKG_PATH"

README_PATH=README.md
echo "Changing README.md..."

sed -i -E "s/___apollo-blockchain-[0-9]{1,}\.[0-9]{1,}\.[0-9]{1,}/___apollo-blockchain-$NEW_VERSION/g" ${README_PATH}
