Set WshShell = CreateObject("WScript.Shell") 
WshShell.Run CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName) & "\run-desktop.bat", 0, false
