set /p VERSION=<VERSION
set WALLETDIR=%1
del /S /F /Q %1\bin
del /S /F /Q %1\sbin
del /F /Q %1\*.jar

del /F /Q %1\lib\apl-*
rem DB download block
rem mkdir %1\tmpdir
rem curl --retry 100 -o %1\tmpdir\%2.tar.gz -k https://apl-db-upd.s3.fr-par.scw.cloud/%2-2021-q4.tar.gz
rem del /S /F /Q %userprofile%\.apl-blockchain\apl-blockchain-db\%2
rem 7z x %1\tmpdir\%2.tar.gz -so | 7z x -ttar -si -y -o"%userprofile%\.apl-blockchain\apl-blockchain-db\"
rem del /S /F /Q %1\tmpdir\%2.tar.gz
rem rmdir %1\tmpdir






