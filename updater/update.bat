set /p VERSION=<VERSION
set WALLETDIR=%1
set TARGETDIR=%2
del /S /F /Q %1\bin
del /S /F /Q %1\sbin
del /S /F /Q %1\*.jar
del /S /F /Q %1\guilib
del /S /F /Q %1\html-stub
del /S /F /Q %1\lib
del /S /F /Q %1\sbin
del /S /F /Q %1\updater
del /S /F /Q %1\webui
del /S /F /Q %1\%1\3RD-PARTY-LICENSES.txt
del /S /F /Q %1\VERSION*
del /S /F /Q %1\LICENSE*
del /S /F /Q %1\META-INF
del /S /F /Q %1\update*
del /S /F /Q %1\..\apollo-web-ui
del /S /F /Q %1\..\apollo-tools

robocopy %1 %1\.. /S
robocopy %2 %1\.. /S

del /S /F /Q %1
rmdir /S /Q %1








