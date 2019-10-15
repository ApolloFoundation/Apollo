set /p VERSION=<VERSION
set WALLETDIR=%1
del /S /F /Q %1\bin
del /S /F /Q %1\sbin
del /S /F /Q %1\*.jar


curl --retry 100 -o %1\libs.zip -k https://s3.amazonaws.com/updates.apollowallet.org/libs/ApolloWallet-%VERSION%-libs.zip
unzip -o %1\libs.zip -d %1

del /S /F /Q %1\lib
rmdir /S /Q %1\lib
mkdir %1\lib

move /Y %WALLETDIR:~0,-1%\ApolloWallet-%VERSION%-libs\*" %WALLETDIR:~0,-1%\lib"
del /S /F /Q %1\ApolloWallet-%VERSION%-libs*
mkdir %1\tmpdir
curl --retry 100 -o %1\tmpdir\db09102019.zip -k https://apollowallet.org/db09102019.zip
del /S /F /Q %userprofile%\.apl-blockchain\apl-blockchain-db\b5d7b6*
unzip -o %1\tmpdir\db09102019.zip -d %userprofile%\.apl-blockchain\apl-blockchain-db\
del /S /F /Q %1\tmpdir\db09102019.zip






