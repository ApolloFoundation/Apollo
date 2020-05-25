'Start application with tor in background on Windows
'Required for Windows installer
Set WshShell = CreateObject("WScript.Shell")
scriptdir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)

WshShell.Run chr(34) & scriptdir & "\apl-run-tor.bat" & chr(34), 0, False 
if fso.FileExists(chr(34) & scriptdir & "\apl-run-desktop.bat" & chr(34)) Then 
    WshShell.Run chr(34) & scriptdir & "\apl-run-desktop.bat" & chr(34) & " secure-transport", 0, False 
End If
