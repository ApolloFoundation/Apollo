'Start application with tor in background on Windows
'Required for Windows installer
Set WshShell = CreateObject("WScript.Shell")
WshShell.Run "run-tor.bat", 0, false
