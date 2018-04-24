set APPLICATION=Apollo
java -cp classes apl.tools.ManifestGenerator
del /f /q %APPLICATION%.jar
jar cvfm %APPLICATION%.jar resource\apl.manifest.mf -C classes\  . && (echo "OK")|| (goto error)
del /f /q %APPLICATION%service.jar
jar cvfm %APPLICATION%service.jar resource\aplservice.manifest.mf -C classes\ . && (echo "OK") || (goto error)

echo "jar files generated successfully"
goto end
:error
echo "Error"
:end