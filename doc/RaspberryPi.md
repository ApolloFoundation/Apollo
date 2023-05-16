== How to run Apollo node on Raspberry PI 2/3

Yes, it is possible and it is easy. First, you need Java 17 JRE or JDK installed on your Raspberry PI.

At the time Raspbian repositories have JDK 9 only so JDK 11  needs to be download from somewhere.

One of possible JKDs for Pi is Zulu from https://www.azul.com/downloads/zulu-embedded/

You need to download Java 17 Arm 32bit JDK on Linux (for Armv8/v7/v6 Hard Float ABI)
It is tar.gz archieve so you have to untar it in some directory, e.g. /usr/local:

cd /usr/local
tar -xvzf /path/to/zuluJdk.tgz

it creates direcory named somewhat like zulu11.1.8-ca-jdk11-c2-linux_aarch32hf
This directory contains JDK 11. It is convinient to create short symlink

ln -s zulu11.1.8-ca-jdk11-c2-linux_aarch32hf zulu

Second, you should to add path to it and set JAVA_HOME
It could be done by editing ~/.bashrc file
Just add following  2 lines to it:

export JAVA_HOME=/usr/local/zulu
export PATH=$JAVA_HONE/bin:$PATH

Logout and login again. Check java version:

java -version

It should say somethink like this:

openjdk version "11" 2018-12-18 LTS
OpenJDK Runtime Environment Zulu11.1+8-CA (build 11+28-LTS)
OpenJDK Server VM Zulu11.1+8-CA (build 11+28-LTS, mixed mode)

Now you need Apoolo distrinution itself. Easieast way is to use cross-patform tar.gz of latest Apollo.
Download it and Untar it to home or any other directory. 

Note, that you have limited disk space on your Raspberry, 
it is just your SD card. So may be it is good idea to connect some USB drive, mount it and use for Apollo DB.
To change Apollo DB location, please use --db-dir command line switch.
 

Well, now just run Apollo by scripts from bin directory of Apollo distribution.
 