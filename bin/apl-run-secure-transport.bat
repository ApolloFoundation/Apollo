@REM Run secure transport on windows. Required for Windows installer.
@echo off
set DIRP=%~dp0
call "%DIRP%\apl-common.bat"
@REM start Apollo

cd %APL_TOP/secureTransport	
start runClient.bat 
cd ..
%JAVA_CMD%  -Dapl.exec.mode=transport -jar "%APL_GUI_MAIN%"
