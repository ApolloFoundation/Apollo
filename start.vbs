Set WshShell = CreateObject("WScript.Shell") 
WshShell.Run CreateObject("Scripting.FileSystemObject").GetParentFolderName(WScript.ScriptFullName) & "\run.bat", 0, false
