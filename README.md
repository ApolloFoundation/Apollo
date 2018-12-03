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
If you have already installed ___jdk 8___ and ___maven___, you can skip __"Preparation steps" section__, but its recommended to __review__ 
your software versions using instructions from __"Preparation steps" section__
## Preparation steps ##
   1. Download [Java Development Kit (jdk), version 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
   2. Setup `JAVA_HOME` variable to point on unpacked jdk if not set
   3. Add to `PATH` variable path to java binaries -> `JAVA_HOME/bin`
      > NOTE: if your computer has jdk 9, jdk 10 or jdk 11, you should remove it from `PATH` variable
   4. Open command line and execute: `java -version`. 
        
        __Output example:__ 
        * java version "1.8.0_161"
        * Java(TM) SE Runtime Environment (build 1.8.0_161-b12)
        * Java HotSpot(TM) 64-Bit Server VM (build 25.161-b12, mixed mode)

        __Parameters definitions:__
        * __java version x.x.x_x__ - ___should start with 1.8!___
        * _JRE_ build version - ___should start with 1.8!___
        * _JVM_ - __64-bit__ is preferable
   5. If ___version matches___ downloaded version, your ___java___ was __installed successfully__ and you can __proceed with next steps_. If __version
   does not
   match__,
   ___delete old version___, _setup
    variables_ (`JAVA_HOME`, `PATH`)_ and try again. __PATH should not contain few java bin directories!__
   6. Download build tool for project - [maven](https://archive.apache.org/dist/maven/maven-3/3.5.2/binaries/) from official site
   7. Unpack maven binaries into convenient folder
   8. Create `M2_HOME` variable or update existing to point to unpacked maven folder
   9. Add to `PATH` variable path to maven binaries __M2_HOME/bin__
   10. Open command line and execute: `mvn -v`. 
   
        __Output example:__
        * Apache Maven 3.5.3 (3383c37e1f9e9b3bc3df5050c29c8aff9f295297;2018-02-24T21:49:05+02:00)
        * Maven home: __E:\binaries\apache-maven-3.5.3\bin\..__
        * Java version: __1.8.0_161__, vendor: Oracle Corporation
        * Java home: __E:\Java\jdk1.8.0_161\jre__
        * Default locale: en_En, platform encoding: Cp1251
        * OS name: "windows 10", version: "10.0", arch: "amd64", family: "windows"
    
        __Parameter definitions:__
        * ___Apache Maven x.x.x___ - version of installed maven ___(should be equals to downloaded version!)___
        * _Maven home_ - path to binaries, ___should be equals to path to maven binaries which you have set in `PATH` variable___
        * ___Java version___ - version of java which __should be equals to downloaded java version__
        * _Java home_ - path to java ___should point to your downloaded java___
   11. If ___maven version, javaHome and java_version___ __matches__ your downloaded java and maven -> your maven was __installed successfully__ and
   you are able to __build and run wallet__! Just choose your OS from the list below and perform specified steps.

## Linux/MacOS
   * Clone repository using git command `git clone` or download archive file of source code
   * Unpack archive of source code if required
   * go to source directory
   * find ___compile.sh___ file and run it
   * find ___run-desktop.sh___ and run it
   * application should start in desktop mode
   * if you require command line mode -> use ___run.sh___ instead of ___run-desktop.sh___

## Windows
   * Clone repository using git command `git clone` or download archive file of source code
   * Unpack archive of source code if required
   * go to source directory
   * find ___compile.bat___ file and run it
   * find ___run-desktop.bat___ and run it
   * application should start in desktop mode
   * if you require command line mode -> use ___run.bat___ instead of ___run-desktop.bat___



