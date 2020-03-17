This directory provides examples of apl-blockchain configuration.
apl-blockchain config consist of two files: apl-blockchain.properties and chains.json
apl-blockchain.properties describe parameters for entire apl-blockchain application, such as db settings and server config
chains.json provides list of chains, which you can use to switch to different chain or create your own chain and then activate it

 ===========COMMON USE CASES====================

 ==========Activate another chain ==============

 1. Open chains.json
 2. Find current active chain, where "active" flag set to "true" and set it to "false"
 3. Find a desired chain and set field "active" to "true"
 4. Save your changes

 ==========Add another chain ===================

 1. Open chains.json
 2. Go to the end of file
 3. Copy full config of previous chain and set up your parameter such as project name, account prefix, coin symbol and other.
 4. Do not forget to add at least one item to "blockchainProperties" array, which set to "0" height. In other case apl-blockchain will not work
 5. Activate your chain as described in "Activate another chain" section above

 =========Configure data migrator ===============

 1. Open apl-blockchain.properties file
 2. Find "MIGRATION section
 3. Set up deletion of application data after successful migration, by default all successfully migrated data will be removed automatically
    When you set to false one of the parameters, chosen data will not be deleted after migration
 4. Save you changes

 =========Configure 2FA storage=================

 1. Open apl-blockchain.properties file
 2. Find "2FA" section
 3. Set "apl.store2FAInFileSystem" to true for storing 2fa data in filesystem
 4. Save your changes

 ======= HOW TO ACTIVATE YOUR CONFIG ===========

To notify apl-blockchain application about your custom config, follow next steps:
1. Now choose one of the most suitable cases to notify apl-blockchain, that you
   want to use custom config
  OPTION 1
    Choose one directory from the list
     a) System config directory (only for UNIX) - /etc/apl-blockchain/
     b) Installation directory, where main Apollo.jar is located
     с) User home directory - only for user mode - /home/user/.apl-blockchain
    Go to chosen directory and create 'conf' folder
  OPTION 2
    Set up environment variable which will point to your custom config folder. Name of the environment variable - APL_BLOCKCHAIN_CONFIG_DIR
  OPTION 3
    Run apl-blockchain application using special cmd parameter '--config_dir' or '-c' and specify path to config folder.
    Example: java -jar Apollo.jar -c /home/user/apl-conf
2. Move your 'apl-blockchain.properties' or 'chains.json' or both  to chosen conf directory
3. Now your 'apl-blockchain.properties' or 'chains.json' or both will be used as configuration of apl-blockchain
   Note, that your changes in your config file will overwrite default application config, but you
   can easily remove unnecessary config values from your config => default values will be used
   instead. When your config does not allow apl-blockchain app to work correctly -> just
   remove your config and restart apl-blockchain.


=== HOW to DOWNLOAD specific, existing shard archive from existing TestNet ===

Preconditions:
TestNet should run with sharding enabled at least 6 nodes (one node can be without sharding).
Those 6 nodes should have 'SHARDing turned ON', shards are exist in network, and they are equal (by hash returned on URL - /rest/shards)

You'll should specify following params for importing shard archive:
--no-shards-import false --no-shards-create false

If you have about 6 nodes with 'equal shard hash' on them, you can try to 'force' download/importing specific shard archive into your local EMPTY node.

-DAPOLLO_DOWNLOAD_SHARD_ID=3
param forces downloading + importing shard archive #3, but shard archive 'hash checking procedure' can FAIL (not enough nodes, hashes are not equal), so downloading can FAIL potentially

-DAPOLLO_FORCE_IMPORT_SHARD_ID=3
param forces shard archive #3 importing ONLY, but shard archive should EXIST locally in a folder, so NO downloading is done, because you should download archive #3 manually
after manual download put it into specific folder for TestNet like:
/apl-blockchain-db/2f2b61/apl-blockchain-export-data/apl-blockchain-shard-3-chain-2f2b6149-d29e-41ca-8c0d-f3343f5540c6.zip

Example running node with params:
java -Xms312m -Xmx1524m -DAPOLLO_DOWNLOAD_SHARD_ID=3 -jar apl-exec-1.42.1.jar -s -d 3 --net 2 --no-shards-import false --no-shards-create false
java -Xms312m -Xmx1524m -DAPOLLO_FORCE_IMPORT_SHARD_ID=3 -jar apl-exec-1.42.1.jar -s -d 3 --net 2 --no-shards-import false --no-shards-create false