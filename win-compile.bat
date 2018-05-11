@echo off

:: possible locations under HKLM\SOFTWARE of JavaSoft registry data
set "javaNativeVersion="
set "java32ON64=Wow6432Node\"

:: for variables
::    %%k = HKLM\SOFTWARE subkeys where to search for JavaSoft key
::    %%j = full path of "Java Development Kit" key under %%k
::    %%v = current jdk version
::    %%e = path to jdk

set "javaDir="
set "javaVersion="
if not exist %JAVA_HOME%\bin\javac.exe (
    echo JAVA_HOME is not set to jdk. Looking for jdk in registry
    for %%k in ( "%javaNativeVersion%" "%java32ON64%") do if not defined javaDir (
        for %%j in (
            "HKLM\SOFTWARE\%%~kJavaSoft\Java Development Kit"
        ) do for /f "tokens=3" %%v in (
            'reg query "%%~j" /v "CurrentVersion" 2^>nul ^| find /i "CurrentVersion"'
        ) do for /f "tokens=2,*" %%d in (
            'reg query "%%~j\%%v" /v "JavaHome"   2^>nul ^| find /i "JavaHome"'
        ) do (
         if exist "%%~e\bin\javac.exe" (
         set "javaDir=%%~e") & set "javaVersion=%%v"
         )
        )
    ) else (
    echo Using JAVA_HOME path to jdk: %JAVA_HOME%
    set "javaDir=%JAVA_HOME%"
)

if not defined javaDir (
    echo JDK not found. Assuming you have disc:\path\to\jdk\bin in your PATH variable
) else (
    echo JDK was found
    echo JDK_HOME="%javaDir%"
    echo JDK_VERSION="%javaVersion%"
    set "javaDir=%javaDir%\bin\"
)

set CP=lib\*;classes
set SP=src\java\
set APPLICATION=Apollo
if exist "%APPLICATION%.jar" (
echo Removing '%APPLICATION%.jar'
    del /Q /F %APPLICATION%.jar
)
if exist "%APPLICATION%service.jar" (
    echo Removing '%APPLICATION%service.jar'
    del /Q /F %APPLICATION%service.jar
)
if exist classes\ (
echo Removing compiled classes 'classes\'
    rd /s /q classes
)
echo Creating directory 'classes\'
    md classes\
if exist addons\classes (
    echo Removing directory 'addons\classes\'
    rd /s /q addons\classes
)
echo Creating directory 'addons\classes\'
md addons\classes\
dir /S src\java\*.java /B > sources.tmp
echo Compiling main sources... 'src\java\*'
"%javaDir%javac.exe" -encoding utf8 -sourcepath %SP% -classpath %CP% -d classes\ @sources.tmp && ( echo Main Apl class files compiled successfully ) || ( goto error )
del /Q /F sources.tmp

dir /S addons\*.java /B > nul 2>&1 && ( echo Addons are present ) || (echo Addons are not present. & goto success)
dir /S addons\src\*.java /B > sources.tmp
echo Compiling addons sources... 'addons\src\*'
"%javaDir%javac.exe" -encoding utf8 -sourcepath %SP% -classpath %CP% -d addons\classes @sources.tmp  && ( echo addon class files compiled successfully & goto success ) || ( goto error)
del /Q /F sources.tmp
:error
echo FAIL! Classes were compiled with errors!
goto end
:success
echo All classes were compiled successfully!
goto end
:end
if exist sources.tmp (
    del /f /q sources.tmp
)