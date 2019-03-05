@REM Run secure transport on windows. Required for Windows installer.
@echo off
set DIRP=%~dp0
call %DIRP%\apl-common.bat
@REM start Apollo

cd %APL_TOP/secureTransport	
start runClient.bat 
cd ..
%JAVA_CMD%  -DsocksProxyHost=10.75.110.1 -DsocksProxyPort=1088 -Dapl.runtime.mode=desktop -Dapl.enablePeerUPnP=false -jar %APL_MAIN%
