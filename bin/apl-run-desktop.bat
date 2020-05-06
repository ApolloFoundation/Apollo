REM This file required to run application with desktop UI on windows
@echo off
set DIRP=%~dp0
call "%DIRP%\apl-common.bat"
@REM start Apollo
IF "%1"=="" (
	%JAVA_CMD% -jar %APL_GUI_MAIN%"
)

IF "%1"=="tor" (
    %JAVA_CMD% -Dapl.exec.mode=tor -jar %APL_GUI_MAIN%"
)

IF "%1"=="secure-transport" (
    %JAVA_CMD% -Dapl.exec.mode=transport -jar %APL_GUI_MAIN%"
)


