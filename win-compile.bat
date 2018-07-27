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
    set "JAVA_HOME=%javaDir%"
)

set CP=target\lib\*;target\classes
set SP=src\main\java\
set APPLICATION=Apollo


echo Compiling main sources... '
call mvn clean package -Dmaven.test.skip=true 2>&1 && ( echo Main Apl class files compiled successfully ) || ( goto error )

RMDIR "lib" /S /Q
DEL "Apollo.jar" /F /Q
COPY /Y "target\Apollo.jar" "Apollo.jar"
XCOPY /Y /E "target\lib" "lib\"

dir /S addons\*.java /B > nul 2>&1 && ( echo Addons are present ) || ( echo Addons are not present. & goto success )
dir /S addons\src\*.java /B > sources.tmp
echo Compiling addons sources... 'addons\src\*'
"%javaDir%\bin\javac.exe" -encoding utf8 -sourcepath %SP% -classpath %CP% -d addons\classes @sources.tmp  && ( echo addon class files compiled successfully & goto success ) || ( goto error)
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