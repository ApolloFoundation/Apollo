@REM Compact the apl-blockchain database

@echo *********************************************************************

@echo * This batch file will compact and reorganize the apl-blockchain db *

@echo * This process can take a long time.  Do not interrupt the batch    *

@echo * file or shutdown the computer until it finishes.                  *

@echo *********************************************************************


@echo off
set DIRP=%~dp0
call %DIRP%\..\bin\apl-common.bat
@REM start Apollo tools
%JAVA_CMD% -jar ${APL_TOOLS} compactdb