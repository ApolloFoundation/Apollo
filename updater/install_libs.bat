set /p VERSION=<VERSION
set WALLETDIR=%1
del /S /F /Q %1\bin
del /S /F /Q %1\sbin
del /F /Q %1\*.jar

del /F /Q %1\lib\apl-*

mkdir %1\tmpdir

curl --retry 100 -o %1\tmpdir\%2.tar.gz -k https://apl-db-upd.s3.fr-par.scw.cloud/%2-2021-q4.tar.gz
del /S /F /Q %userprofile%\.apl-blockchain\apl-blockchain-db\%2
7z x %1\tmpdir\%2.tar.gz -so | 7z x -ttar -o"%userprofile%\.apl-blockchain\apl-blockchain-db\"
del /S /F /Q %1\tmpdir\%2.tar.gz
rmdir %1\tmpdir






