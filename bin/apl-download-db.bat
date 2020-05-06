@REM This file required for starting user-mode Apollo application on Windows.
@echo off
set DIRP=%~dp0
call "%DIRP%\apl-common.bat"

@REM Start downloading Apollo database. Please make sure that Apollo IS SPOPPED!

set UH=%HOMEDRIVE%%HOMEPATH%
set DB_LOCATION=%UH%\.apl-blockchain\apl-blockchain-db
rmdir /Q/S %DB_LOCATION%
mkdir %DB_LOCATION%
set DB_FILE=b5d7b6.jar
set DB_URL="https://s3.amazonaws.com/updates.apollowallet.org/database/%DB_FILE%"
echo "Downloading and Exctracting %DB_FILE% into %DB_LOCATION%"
cd %DB_LOCATION%
"%DIRP%\..\curl" -k --output %DB_FILE% %DB_URL%
%JAR_CMD% -v -x -f %DB_FILE%
@REM DEL META-INF
@REM DEL %DB_FILE%



