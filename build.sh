#!/bin/bash

VERSION=$(head -n1 VERSION)

rm -rf build
./mvnw -DskipTests clean package
mkdir build

cp apl-exec/target/apollo-blockchain* build

cd build
unzip apollo-blockchain-${VERSION}.zip
unzip apollo-blockchain-ext-deps-1.0.0.zip
tar -czvf ApolloWallet-${VERSION}.tar.gz ApolloWallet
rm -f *json *sha256 *.zip
