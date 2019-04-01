

Passphrase: candy maple cake sugar pudding cream honey rich smooth crumble sweet treat

#### How to run docker container. ####
1) -> cd /bin/etherium/geth-devnet
2) -> docker build -t eth_private .        // docker build {image name} {Path to Dockerfile}
3) -> docker run --name ethereum-node_dev -p 8545:8545 -p 30303:30303 eth_private


#### Can be useful. ####
1) -> geth attach

2) Geth create new account 
-> geth account new --datadir /data/

3) List accounts 
-> eth.accounts

4) One account 
-> eth.accounts[0]
-> eth.accounts[1]

5) Get account balance 
-> eth.getBalance(eth.accounts[0])

6) Unlock account 
-> personal.unlockAccount(eth.accounts[0])

7) Send money 
-> eth.sendTransaction({from:eth.accounts[0], to:eth.accounts[1], value: web3.toWei(1000000, "ether")})
