@REM This file required for starting user-mode Apollo application on Windows.
@echo off
set DIRP=%~dp0
call "%DIRP%\apl-common.bat"
@REM start Apollo
%JAVA_CMD% -jar %APL_MAIN%
