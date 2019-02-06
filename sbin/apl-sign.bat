@REM Sign transaction offline and manually
@echo *********************************************************************
@echo * This shell file will sign transaction json stored in file and     *
@echo * write signed transaction to output file. Works in combined mode   *
@echo *********************************************************************
@echo off
set DIRP=%~dp0
call %DIRP%\..\bin\apl-common.bat
@REM start Apollo tools
%JAVA_CMD% -jar %APL_TOOLS% signtx --json
