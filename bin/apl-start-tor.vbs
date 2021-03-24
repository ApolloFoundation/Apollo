'Start application with tor in background on Windows
'Required for Windows installer
Set WshShell = CreateObject("WScript.Shell")
scriptdir = CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName)

WshShell.Run chr(34) & scriptdir & "\apl-run-tor.bat" & chr(34), 0, False 
Set fso = CreateObject("Scripting.FileSystemObject")
if fso.FileExists(scriptdir & "\..\..\apollo-desktop\bin\apl-run-desktop.bat") Then 
    WshShell.CurrentDirectory = scriptdir & "\..\..\apollo-desktop\bin\"
    WshShell.Run chr(34) & "apl-run-desktop.bat" & chr(34) & " tor", 0, False 
End If
