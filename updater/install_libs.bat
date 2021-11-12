set /p VERSION=<VERSION
set WALLETDIR=%1
del /S /F /Q %1\bin
del /S /F /Q %1\sbin
del /F /Q %1\*.jar

rem curl --retry 100 -o %1\libs.zip -k https://s3.amazonaws.com/updates.apollowallet.org/libs/ApolloWallet-%VERSION%-libs.zip
rem unzip -o %1\libs.zip -d %1

del /F /Q %1\lib\apl-*
rem rmdir /S /Q %1\lib
rem mkdir %1\lib

rem move /Y %WALLETDIR:~0,-1%\ApolloWallet-%VERSION%-libs\*" %WALLETDIR:~0,-1%\lib"
rem del /S /F /Q %1\ApolloWallet-%VERSION%-libs*
rem mkdir %1\tmpdir

curl --retry 100 -o %1\tmpdir\%2.zip -k https://apl-db-upd.s3.fr-par.scw.cloud/%2-2021-q4.zip
del /S /F /Q %userprofile%\.apl-blockchain\apl-blockchain-db\%2
unzip -o %1\tmpdir\%2.zip -d %userprofile%\.apl-blockchain\apl-blockchain-db\
del /S /F /Q %1\tmpdir\%2.zip







