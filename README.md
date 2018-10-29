# Apollo Blockchain Platform 
Apollo is being being developed by the Apollo Foundation and supporting members of the community.

## Requirements
Java 8 is required to run the desktop clients.



# Links #
- [website](https://Apollocurrency.com)
- [twitter](https://Twitter.com/Apollocurrency)
- [telegram](https://T.me/apollocommunity)
- [facebook](https://www.facebook.com/Apolloprivacycoin)
- [youtube](https://www.youtube.com/channel/UCZbB3PAUlkSKuBYEMG-l_CQ)


# Specifications #


    Concensus: POS (Proof of Stake)
    
    Total Supply: 21 Billion
    
    Circulating: 15 Billion
    
    Mining: Pre-Mined
    
    Inflation: 0%

# Build instruction #
If you have ___jdk 8___ and ___maven___, you can skip __Preparation steps section__, but its recommended at least to __review__ your software versions
 using
instructions from
__Preparation steps section__
## Preparation steps: ##
   * Clone repository using git command `git clone` or download zip file of source code
   * Unpack archive of source code if required
   * Download [Java Development Kit (jdk), version 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
   * Setup `JAVA_HOME` variable to point on unpacked jdk if not set
   * Add to `PATH` variable path to java binaries -> JAVA_HOME/bin
     > NOTE: if your computer has jdk 9 or jdk 10 or jdk 11, you should remove it from path
   * Open command line and execute: `java -version`. Output example:
         1. java version "1.8.0_161" - should start with 1.8!
         2. Java(TM) SE Runtime Environment (build 1.8.0_161-b12)
         3. Java HotSpot(TM) 64-Bit Server VM (build 25.161-b12, mixed mode)
   ##### Parameters definitions:
    1. __java version x.x.x_x__ - ___should start with 1.8!___
    2. _JRE_ build version - ___should start with 1.8!___
    3. _JVM_ - __64-bit__ is preferable
   * If ___version matches___ downloaded version, your ___java___ was __installed successfully__ and you can __proceed with next steps_. If __version
   does not
   match__,
   ___delete old version___, _setup
    variables_ __(JAVA_HOME, PATH)__ and try again. __PATH should not contain few java bin directories!__
   * Download build tool for project - [maven](https://archive.apache.org/dist/maven/maven-3/3.5.2/binaries/) from official site
   * Unpack maven binaries into convenient folder
   * Create M2_HOME variable or update existing to point to unpacked maven folder
   * Add to PATH variable path to maven binaries M2_HOME/bin
   * Open command line and execute: `mvn -v`. Output example:
         1. Apache Maven 3.5.3 (3383c37e1f9e9b3bc3df5050c29c8aff9f295297;2018-02-24T21:49:05+02:00)
         2. Maven home: E:\binaries\apache-maven-3.5.3\bin\..
         3. Java version: 1.8.0_161, vendor: Oracle Corporation
         4. Java home: E:\Java\jdk1.8.0_161\jre
         5. Default locale: en_En, platform encoding: Cp1251
         6. OS name: "windows 10", version: "10.0", arch: "amd64", family: "windows"
    ##### Parameter definitions:
    1. ___Apache Maven x.x.x___ - version of installed maven ___(should be equals to downloaded version!)___
    2. _Maven home_ - path to binaries, ___should be equals to path to maven binaries which you have set in `PATH` variable___
    3. ___Java version___ - version of java which __should be equals to downloaded java version__
    4. _Java home_ - path to java ___should point to your downloaded java___
   * If ___maven version, javaHome and java_version___ __matches__ your downloaded java and maven -> your maven was __installed successfully__ and
   you are able to __build and run
   wallet__!

## Linux/MacOS
   * go to source directory
   * find ___compile.sh___ file and run it
   * find ___run-desktop.sh___ and run it
   * application should start in desktop mode
   * if you require command line mode -> use ___run.sh___ instead of ___run-desktop.sh___

## Windows

   * go to source directory
   * find ___compile.bat___ file and run it
   * find ___run-desktop.bat___ and run it
   * application should start in desktop mode
   * if you require command line mode -> use ___run.bat___ instead of ___run-desktop.bat___



