# Apollo Blockchain Platform 

Apollo is being developed by the Apollo Foundation and supporting members of the community.


## Requirements
Java 11 is required to run the desktop clients.

# Links #
- [website](https://Apollocurrency.com)
- [twitter](https://Twitter.com/Apollocurrency)
- [telegram](https://T.me/apollocommunity)
- [facebook](https://www.facebook.com/Apolloprivacycoin)
- [youtube](https://www.youtube.com/channel/UCZbB3PAUlkSKuBYEMG-l_CQ)

- [releases and installers] (https://github.com/ApolloFoundation/Apollo/releases)

# Specifications #


    Concensus: POS (Proof of Stake)
    
    Total Supply: 21 Billion
    
    Circulating: 15 Billion
    
    Mining: Pre-Mined
    
    Inflation: 0%

# Build instruction #

If you have already installed ___jdk 11___, you can skip __"Preparation steps" section__, but its recommended to __review__ 
your software versions using instructions from __"Preparation steps" section__

## Preparation steps ##
   1. Download [Java Development Kit (jdk), version 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html)
   2. Setup `JAVA_HOME` variable to point on unpacked jdk if not set
   3. Add to `PATH` variable path to java binaries -> `JAVA_HOME/bin`
      > NOTE: if your computer has jdk 8, jdk 9 or jdk 10, you should remove it from `PATH` variable
   4. Open command line and execute: `java -version`. 
        
    Output example: 
<pre>
  java version "11.0.2" 2019-01-15 LTS<br>
  Java(TM) SE Runtime Environment 18.9 (build 11.0.2+9-LTS)<br>
  Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.2+9-LTS, mixed mode)
</pre>

   5. If ___version matches___ downloaded version, your ___java___ was __installed successfully__ and you can __proceed with next steps_. If __version
   does not
   match__,
   ___delete old version___, _setup
    variables_ (`JAVA_HOME`, `PATH`)_ and try again. __PATH should not contain few java bin directories!__
   6. Open command line, change your current directory to this project root and execute
   
- for Linux:
```shell script
./mvnw -v
```
- for Windows cmd:
```cmd
mvnw -v
```
     
    Output example:
<pre>
  Apache Maven 3.6.2 (40f52333136460af0dc0d7232c0dc0bcf0d9e117; 2019-08-27T18:06:16+03:00)<br>
  Maven home: /home/your_user_name/.m2/wrapper/dists/apache-maven-3.6.2-bin/795eh28tki48bv3l67maojf0ra/apache-maven-3.6.2<br>
  Java version: 11.0.2, vendor: Oracle Corporation, runtime: /usr/java/jdk-11.0.2<br>
  Default locale: en_US, platform encoding: UTF-8<br>
  OS name: "linux", version: "4.20.16-200.fc29.x86_64", arch: "amd64", family: "unix"<br>
</pre>
   7. If ___maven version, javaHome and java_version___ __matches__ your downloaded java then
   you are able to __build and run wallet__! Just choose your OS from the list below and perform specified steps.

## Linux/MacOS
   * Clone repository using git command `git clone` or download archive file of source code
   * go to source directory
   * run ___mvn install___ 
   * go to bin directory and run ___apl-run-desktop.sh___ 
   * application should start in desktop mode
   * if you require command line mode -> use ___apl-run.sh___ instead of ___apl-run-desktop.sh___

## Windows
   * Clone repository using git command `git clone` or download archive file of source code
   * go to source directory
   * run ___mvn install___ 
   * go to bin directory and run ___apl-run-desktop.bat___
   * application should start in desktop mode
   * if you require command line mode -> use ___apl-run.bat___ instead of ___apl-run-desktop.bat___

## Command-line options

apl-exec.jar and all scripts accept command line options. To get list of available options, run with --help switch.
Most important options:

        --net, -n index of network to run with. 0 is main net, 1 is 1st public test net with stable release,
        2 is 2nd testnet with development/staging code and 3 is 3rd test net with experimental features
        --testnet means run with 1st test net. Higher priority then --net switch

        --debug, -d  from 0 to 4. 0 is ERROR level of logs, 4 is TRACE

Example:

    bin/apl-run.sh -d 4 -n 2

This command runs blockchain application with 2nd test net and debug level TRACE

## IDE

Project is entirely on Maven v3 and could be loaded in any IDE that supports Maven.


## DEX

#### Generate eth smart contract class.

`$ web3j solidity generate --javaTypes -b dex.bin -a dex.abi -o $Path/Apollo/apl-core/src/main/java/com/apollocurrency/aplwallet/apl/eth/contracts/ -p com.apollocurrency.aplwallet.apl.eth.contracts`