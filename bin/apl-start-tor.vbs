'Start application with tor in background on Windows
'Required for Windows installer
Set WshShell = CreateObject("WScript.Shell")
scriptdir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)
WshShell.Run chr(34) & scriptdir & "\apl-run-desktop.bat" & chr(34) & " tor" 
