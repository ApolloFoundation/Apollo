# Apollo Blockchain Platform 
<<<<<<< HEAD

Apollo is being being developed by the Apollo Foundation and supporting members of the community.
=======
Apollo is being developed by the Apollo Foundation and supporting members of the community.
>>>>>>> master

## Requirements
Java 11 is required to run the desktop clients.

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

If you have already installed ___jdk 11___ and ___maven___, you can skip __"Preparation steps" section__, but its recommended to __review__ 
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
   6. Download build tool for project - [maven](http://maven.apache.org/download.cgi) from official site
   7. Unpack maven binaries into convenient folder
   8. Create `M2_HOME` variable or update existing to point to unpacked maven folder
   9. Add to `PATH` variable path to maven binaries __M2_HOME/bin__
   10. Open command line and execute: `mvn -v`. 
   
    Output example:
<pre>
  Apache Maven 3.6.0 (97c98ec64a1fdfee7767ce5ffb20918da4f719f3; 2018-10-24T21:41:47+03:00)<br>
  Maven home: /usr/local/maven  Maven home: /usr/local/maven<br>
  Java version: 11.0.2, vendor: Oracle Corporation, runtime: /usr/java/jdk-11.0.2<br>
  Default locale: en_US, platform encoding: UTF-8<br>
  OS name: "linux", version: "4.20.16-200.fc29.x86_64", arch: "amd64", family: "unix"<br>
</pre>
   11. If ___maven version, javaHome and java_version___ __matches__ your downloaded java and maven -> your maven was __installed successfully__ and
   you are able to __build and run wallet__! Just choose your OS from the list below and perform specified steps.

## Linux/MacOS
   * Clone repository using git command `git clone` or download archive file of source code
   * go to source directory
   * run ___mvn install___ 
   * go to bin directory and run ___apl-run-desktop.sh___ 
   * application should start in desktop mode


## Windows
   * Clone repository using git command `git clone` or download archive file of source code
   * go to source directory
   * run ___mvn install___ 
   * go to bin directory and run ___apl-run-desktop.bst___
   * application should start in desktop mode
   * if you require command line mode -> use ___run.bat___ instead of ___run-desktop.bat___


