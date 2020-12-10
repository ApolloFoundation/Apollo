# Apollo Blockchain Platform  Core

Apollo is being developed by the Apollo Foundation and supporting members of the community.

This repository contains core classes of Apollo blockchain platform. For executable builds please refer Apollo-exec repository.

## Requirements
Java 11 is required to run the desktop clients.

##Other modules required to build Apollo components

    1. Apollo-bom-ext
    2. Apollo-web-ui
    3. Apollo-exec
    

# Build instruction #

If you have already installed ___jdk 11___, you can skip __"Preparation steps" section__, but its recommended to __review__ 
your software versions using instructions from __"Preparation steps" section__

## Preparation steps ##
   1. Download [Java Development Kit (jdk), version 11](https://www.oracle.com/technetwork/java/javase/downloads/jdk11-downloads-5066655.html)
   2. Setup `JAVA_HOME` variable to point on unpacked jdk if not set
   3. Add to `PATH` variable path to java binaries -> `$JAVA_HOME/bin`
      > NOTE: if your computer has jdk 8, jdk 9 or jdk 10, you should remove path from `PATH` variable
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
   * run `./build-all.sh` (or `./build-all.sh -DskipTests` for skipping tests)

## Windows
   * Clone repository using git command `git clone` or download archive file of source code
   * go to source directory
   * run `build-all.bat` (or `build-all.bat -DskipTests` for skipping tests)  

## IDE

Project is entirely on Maven v3 and could be loaded in any IDE that supports Maven.

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
    