#!/usr/bin/env bash

echo "Init"

/geth --datadir ~/.etherium/mychain/data/ init ~/config/CustomGenesis.json

echo "Run node"

/geth --identity "ApoloTestNode" --rpc --rpcport 8545 --port 30303 --rpccorsdomain "*" --networkid 454545 --datadir ~/.etherium/mychain/data/ --nodiscover --rpcaddr "0.0.0.0" --wsaddr "0.0.0.0" --rpcapi "db,eth,net,web3,personal" --mine --minerthreads 1