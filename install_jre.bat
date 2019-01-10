set /p VERSION=<VERSION
set WALLETDIR=%1

del /S /F /Q %1\jre
rmdir /S /Q %1\jre

curl -o %1\jre.zip -k https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_windows-x64_bin.zip 
unzip -o %1\jre.zip -d %1 

move /Y %WALLETDIR:~0,-1%\jdk-11.0.1" %WALLETDIR:~0,-1%\jre"

curl -o %1\libs.zip -k https://s3.amazonaws.com/updates.apollowallet.org/libs/ApolloWallet-%VERSION%-libs.zip
unzip -o %1\libs.zip -d %1

del /S /F /Q %1\lib
rmdir /S /Q %1\lib
mkdir %1\lib

move /Y %WALLETDIR:~0,-1%\ApolloWallet-%VERSION%-libs\*" %WALLETDIR:~0,-1%\lib"
