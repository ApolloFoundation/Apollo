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
if not exist %JAVA_HOME%\bin\jar.exe (
    if not exist %JAVA_HOME%\bin\java.exe (
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
set APPLICATION=Apollo
echo Running manifest generator
"%javaDir%java.exe" -cp classes com.apollocurrency.aplwallet.apl.tools.ManifestGenerator && ( echo OK )|| ( goto error )
if exist "%APPLICATION%.jar" (
    echo Removing '%APPLICATION%.jar'
    del /f /q %APPLICATION%.jar
)
echo Building '%APPLICATION%.jar'
"%javaDir%jar.exe" cvfm %APPLICATION%.jar resource\apl.manifest.mf -C classes\ . > nul 2>&1 && ( echo OK )|| ( goto error )
if exist "%APPLICATION%service.jar" (
    echo Removing '%APPLICATION%service.jar...'
    del /f /q %APPLICATION%service.jar
)
echo Building '%APPLICATION%service.jar...'
"%javaDir%jar.exe" cvfm %APPLICATION%service.jar resource\aplservice.manifest.mf -C classes\ . > nul 2>&1 && ( echo OK ) || ( goto error )

echo Jar files were generated successfully!
goto end
:error
echo FAIL! Error was occurred while executing script. See logs above.
:end