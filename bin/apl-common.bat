@echo off
@REM common functions for Apollo scrtips
@echo *********** APOLLO common **********
set MIN_JAVA=110
set apl_bin=%~dp0
call :getTop %apl_bin%
goto cont1
:getTop
for %%i in ("%~dp1..") do set "APL_TOP=%%~fi"
:cont1

if exist %APL_TOP%\jre ( 
	set JAVA_HOME=%APL_TOP%\jre
	set JAVA_CMD=%APL_TOP%\jre\bin\java
) else (
	set JAVA_CMD=java	
)
@REM determine Java version
PATH %JAVA_HOME%\bin\;%PATH%
for /f tokens^=2-5^ delims^=.-_^" %%j in ('java -fullversion 2^>^&1') do set "java_ver=%%j%%k"

if  %java_ver% LSS %MIN_JAVA% (
   @echo WARNING! Java version is less then 11. Programs will not work!	
) else (
   @echo Java version is OK.	
) 

@REM are we in dev env or in production
if exist %APL_TOP%\Apollo.jar (
	set APL_MAIN=%APL_TOP%\Apollo.jar
	set APL_LIB=%APL_TOP%\lib
) else (
        set APL_MAIN=%APL_TOP%\apl-exec\target\Apollo.jar
	set APL_LIB=%APL_TOP%\apl-exec\target\lib
)
for /f tokens^=2-5^ delims^=.-_^" %%j in ('dir /B %APL_LIB%\apl-tools*') do set "APL_VER=%%k.%%l.%%m"
set APL_TOOLS=%APL_LIB%\apl-tools-%APL_VER%.jar
