Set WshShell = CreateObject("WScript.Shell") 
WshShell.Run chr(34) & CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName) & "\run-desktop.bat" & chr(34), 0, false
