
# Apollo Blockchain Platform  Core

## Disclaimer.
Apollo team is actively working on modularity of Apollo blockhain so build scripts and source structure is subject of changes. Apollo project consists of several modules written in different programming languages. If you are not an expert in Java and Maven, JavaScript, NodeJs, npm, yarn, C/C++ and Cmakle please use our release builds at [Apollo Releases page](https://github.com/ApolloFoundation/Apollo/releases).

If you feel like an expert, please use build instructions below. But also please note that instructions may be slightly outdated, escpecially in "development" branches of the Apollo project repositroies.


Apollo is being developed by the Apollo Foundation and supporting members of the community.

This repository contains core classes of Apollo blockchain platform and main executable of Apollo-blockchain component. 

There are other components that are parts of Apollo:

1. [Apollo-web-ui](https://github.com/ApolloFoundation/Apollo-web-ui): Web wallet UI that is served by Apollo blockchain node and can be accersed by browser at http://localhost:7876
2. [Apollo-dektop](https://github.com/ApolloFoundation/Apollo-desktop) Desktop wallet UI. Apollo-web-ui must be installed tobe able to run Apollo desktop wallet.
3. [Apollo-tools](https://github.com/ApolloFoundation/Apollo-tools): "swiss knife" of tools for node maintenance, transaction signing, etc.
4. [Apollo-bom-ext](https://github.com/ApolloFoundation/Apollo-bom-ext) This module required in compilation time oly. It contains BOM of all external libraries used by Apollo components.

## Requirements

Java 11 (JRE) is required to run the most Apollo components.


# Build instruction #

## Java versions

We use LTS JDK version 11 in the development and and in the production environments. To be exact, we use vanilla openjdk builds from AdoptOpenJDK site: [Java Development Kit openjdk v.11 from AdopOpenJDK, ](https://adoptopenjdk.net/). You can download and install JDK for your platform by the link. 

Apollo code runs well on latest JDKs but is not thouroughly tested. So you can use latest JDKs, e.g. version 15 on your own risk.

If you are dveloper, we'd like to recommend [SDKMan kit](https://sdkman.io/) to manage installed Java platforms.  

## Preparation steps ##

   1. Verify your JDK installation by running `java -version` in console:
   
    Output example: 
<pre>
openjdk version "11.0.9.1" 2020-11-04
OpenJDK Runtime Environment 18.9 (build 11.0.9.1+1)
OpenJDK 64-Bit Server VM 18.9 (build 11.0.9.1+1, mixed mode, sharing)
</pre>

   2. If `version` matches 11.0.x version, your ___java___ was __installed successfully__ and you can __proceed with next steps_. 

   3. Clone this repository	

   
   4. Open command line, change your current directory to this project root and execute
   
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
Apache Maven 3.6.2 (40f52333136460af0dc0d7232c0dc0bcf0d9e117; 2019-08-27T18:06:16+03:00)
Maven home: /home/al/.m2/wrapper/dists/apache-maven-3.6.2-bin/795eh28tki48bv3l67maojf0ra/apache-maven-3.6.2
Java version: 11.0.9.1, vendor: Red Hat, Inc., runtime: /usr/lib/jvm/java-11-openjdk-11.0.9.11-4.fc33.x86_64
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "5.9.12-200.fc33.x86_64", arch: "amd64", family: "unix"

</pre>
   7. If output looks similar to example, you can perform build step.

### Linux/MacOS
   * run `./mvnw` (or `./mvnc -DskipTests` for skipping tests)

### Windows
   * run ``mvnw.bat` (or `mvnw.bat -DskipTests` for skipping tests)  

###Other modules required to build Apollo components

    Module  [Apollo-bom-ext](https://github.com/ApolloFoundation/Apollo-bom-ext) contains all external library dependencies used by Apollo blockchain. Usually you do not need to build it because it is in out public artefact repository and downloaded by build scipts authamtically. But if you are developer and have neccessity to change external dependency, please refer to this module.
    
### Installation artefacts

Final artefact that is ready to install and run is loacated in the ___apl-exec/target___ directory and has name like  ___apollo-blockchain-1.47.5-NoOS-NoArch.zip___.
Unzip it to some location and run by scripts in ___ApolloWallet/apollo-blockchain/bin___ directory.

You'll probably need __Apollo-web-ui__ and __Apollo-desktop__ components to use Apollo wallet.
Please follow instructions in [Apollo-web-ui](https://github.com/ApolloFoundation/Apollo-web-ui) and [Apollo-desktop](https://github.com/ApolloFoundation/Apollo-desktop) project epositories.

### IDE

Project is entirely on Maven v3 and could be loaded in any IDE that supports Maven.

## GIT branches

We follow GIT FLOW procedure in our developemnt and use following branhces:

__master__ branch contains stable code of the latest release. It is also tagged for each public release. Please see "Tags" in the "barcnh" dropdown menu. Please use this branch to compile Apollo components.

__develop__ branch contains latest development version. Use this branch if you are developer/contributor.

__stage__ branch contains release preparation work of the last release. Do not use this branch if you are not release engineer


fix/*, feature/*, bugfix/** - temporary branches used by developers. Ususaly those branmches get merged to ___develop___ and deleted after work is done.

## MariaDB

### Initiate
    1) Open repository apl-updater2
    
    2) Run script depends on OS. 
        mariadb-pkg/maria_db_linux_pkg.sh
        mariadb-pkg/maria_db_osx_pkg.sh
        mariadb-pkg/maria_db_windows_pkg.sh
        
    3) Unzip packege and start db installation process. (..../ApolloWallet/apollo-mariadb is a basedir path)
    
    4) Create mariadb config. 
    
            [client-server]
            port=3366
            
            [mysqld]
            # Only allow connections from localhost
            bind-address = 127.0.0.1
            lower_case_table_names=2
            default-storage-engine=rocksdb
            max_connections=1024
            
            datadir=/Users/user/.apl-blockchain/apl-blockchain-db/data
            tmpdir=/Users/user/.apl-blockchain/apl-blockchain-db/tmp
            socket=/Users/user/.apl-blockchain/apl-blockchain-db/mariadb.sock
            log-error=/Users/user/.apl-blockchain/apl-blockchain-db/mariadb.log
            pid-file=/Users/user/.apl-blockchain/apl-blockchain-db/mariadb.pid
            
            basedir=/Users/user/projects/apolo/apl-updater2/mariadb-pkg/target/ApolloWallet/apollo-mariadb
            
            [mariadb]
            plugin_load_add = ha_rocksdb
            rocksdb_max_row_locks=1073741824
                
    
    5) Run init script (Appolo project)
    
        ./bin/maria-db-init.sh {basedir} {mariadb_apollo_instance.cnf}
    Example: sh ./bin/maria-db-init.sh /../../..../apl-updater2/mariadb-pkg/target/ApolloWallet/apollo-mariadb /..../..../.apl-blockchain/apl-blockchain-db/conf/mariadb_apollo_instance.cnf
   

## DOCKER Installation

#### On Linux
https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-ubuntu-20-04-ru
should be completed

Article about MariaDB in docker
https://mariadb.com/kb/en/installing-and-using-mariadb-via-docker/

### How create local docker image for unit tests
See [creation local Docker image link](/unit-test-Docker-Image/README.md)

#### Check IP table / firewall settings to access docker
https://github.com/testcontainers/testcontainers-java/issues/572#issuecomment-517831833

$ sudo iptables -L
```
Chain INPUT (policy ACCEPT)
target     prot opt source               destination         
ACCEPT     all  --  172.17.0.0/24        anywhere
```            
    