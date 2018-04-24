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
set APPLICATION=Apollo
java -cp classes apl.tools.ManifestGenerator
echo Removing '%APPLICATION%.jar'
del /f /q %APPLICATION%.jar
echo Building '%APPLICATION%.jar'
jar cvfm %APPLICATION%.jar resource\apl.manifest.mf -C classes\ . > nul 2>&1 && (echo OK)|| (goto error)
echo Removing '%APPLICATION%service.jar...'
del /f /q %APPLICATION%service.jar
echo Building '%APPLICATION%service.jar...'
jar cvfm %APPLICATION%service.jar resource\aplservice.manifest.mf -C classes\ . > nul 2>&1 && (echo OK) || (goto error)

echo Jar files were generated successfully!
goto end
:error
echo FAIL! Error was occurred while executing script. See logs above.
:end