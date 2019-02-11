@REM Start mint worker
@echo ***********************************************************************
@echo * This batch file will start Apoolo with minting worker for           *
@echo * mintable currencies   						    *
@echo * Take a look at "Mint" section in apl.properties for detailed config *
@echo ***********************************************************************
@echo off
set DIRP=%~dp0
call %DIRP%\..\bin\apl-common.bat
@REM start Apollo
%JAVA_CMD% -jar %APL_MAIN% --mint
