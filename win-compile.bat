set CP=lib\*;classes
set SP=src\java\
set APPLICATION=apl-clone

rd /s /q %APPLICATION%.jar
rd /s /q %APPLICATION%service.jar
rd /s /q classes
md classes\
rd /s /q addons\classes
md addons\classes\

javac -encoding utf8 -sourcepath %SP% -classpath %CP% -d classes\ src\java\apl\*.java src\java\apldesktop\*.java src\java\apl\addons\*.java src\java\apl\crypto\*.java src\java\apl\db\*.java src\java\apl\env\*.java src\java\apl\env\service\*.java src\java\apl\http\*.java src\java\apl\mint\*.java src\java\apl\peer\*.java src\java\apl\tools\*.java src\java\apl\util\*.java  && ( echo "apl class files compiled successfully") || ( goto error )


:check JDK existing
dir addons\*.java > nul 2>&1 && ( echo "Addons present") || (echo "Addons not present. Exit." & goto success)
javac -encoding utf8 -sourcepath %SP% -classpath %CP% -d addons\classes addons\src\*.java  && ( echo "addon class files compiled successfully" goto success ) || ( goto error)

:error
echo "classes are compiled with errors!"
goto end
:success
echo "all classes are compiled successfully!"
goto end
:end
	pause