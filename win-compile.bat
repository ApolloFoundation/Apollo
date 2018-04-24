@echo off
setlocal enableextensions disabledelayedexpansion

:: possible locations under HKLM\SOFTWARE of JavaSoft registry data
set "javaNativeVersion="
set "java32ON64=Wow6432Node\"

:: for variables
::    %%k = HKLM\SOFTWARE subkeys where to search for JavaSoft key
::    %%j = full path of "Java Runtime Environment" key under %%k
::    %%v = current java version
::    %%e = path to java

set "javaDir="
set "javaVersion="
for %%k in ( "%javaNativeVersion%" "%java32ON64%") do if not defined javaDir (
    for %%j in (
        "HKLM\SOFTWARE\%%~kJavaSoft\Java Runtime Environment"
    ) do for /f "tokens=3" %%v in (
        'reg query "%%~j" /v "CurrentVersion" 2^>nul ^| find /i "CurrentVersion"'
    ) do for /f "tokens=2,*" %%d in (
        'reg query "%%~j\%%v" /v "JavaHome"   2^>nul ^| find /i "JavaHome"'
    ) do ( set "javaDir=%%~e" & set "javaVersion=%%v" )
)

if not defined javaDir (
    echo Java not found
    goto end
) else (
    echo Java was found
    echo JAVA_HOME="%javaDir%"
    echo JAVA_VERSION="%javaVersion%"
)
endlocal

set CP=lib\*;classes
set SP=src\java\
set APPLICATION=apl-clone

echo Removing '%APPLICATION%.jar'
rd /s /q %APPLICATION%.jar
echo Removing '%APPLICATION%service.jar'
rd /s /q %APPLICATION%service.jar
echo Removing compiled classes 'classes\'
rd /s /q classes
echo Creating directory 'classes\'
md classes\
echo Removing directory 'addons\classes\'
rd /s /q addons\classes
echo Creating directory 'addons\classes\'
md addons\classes\
echo Compiling main sources... 'src\java\*'
javac -encoding utf8 -sourcepath %SP% -classpath %CP% -d classes\ src\java\apl\*.java src\java\apldesktop\*.java src\java\apl\addons\*.java src\java\apl\crypto\*.java src\java\apl\db\*.java src\java\apl\env\*.java src\java\apl\env\service\*.java src\java\apl\http\*.java src\java\apl\mint\*.java src\java\apl\peer\*.java src\java\apl\tools\*.java src\java\apl\util\*.java  && ( echo Main Apl class files compiled successfully ) || ( goto error )


dir addons\*.java > nul 2>&1 && ( echo Addons are present ) || (echo Addons are not present. & goto success)
echo Compiling addons sources... 'addons\src\*'
javac -encoding utf8 -sourcepath %SP% -classpath %CP% -d addons\classes addons\src\*.java  && ( echo addon class files compiled successfully goto success ) || ( goto error)

:error
echo FAIL! Classes were compiled with errors!
goto end
:success
echo All classes were compiled successfully!
goto end
:end