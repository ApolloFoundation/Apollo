@echo off
set DIRP=%~dp0
call %DIRP%\apl-common.bat
@REM start Apollo

cd %APL_TOP
	
echo ClientTransportPlugin obfs4 exec %cd%\tor\tor\obfs4proxy > %systemdrive%%homepath%\torrc
echo GeoIPFile %cd%\tor\data\tor\geoip >> %systemdrive%%homepath%\torrc
echo GeoIPv6File %cd%\tor\data\tor\geoip6 >> %systemdrive%%homepath%\torrc
echo RunAsDaemon 1 >> %systemdrive%%homepath%\torrc
start tor\tor\tor.exe -f %systemdrive%%homepath%\torrc

%JAVA_CMD%  jar -DsocksProxyHost=localhost -DsocksProxyPort=9050 -Dapl.runtime.mode=desktop  -jar %APL_MAIN%

:endProcess 
	endlocal
