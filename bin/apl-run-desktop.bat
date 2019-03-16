REM This file required to run application with desktop UI on windows
@echo off
set DIRP=%~dp0
call "%DIRP%\apl-common.bat"
@REM start Apollo
%JAVA_CMD% -jar "%APL_GUI_MAIN%"