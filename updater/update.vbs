' This script is used by Updater for Windows to update application directory
' Update will not started, when user did not grant admin privileges to this script
Set fso = CreateObject("Scripting.FileSystemObject")
const ReadOnly = 1
If Wscript.Arguments.Count > 1 Then
   For i = 0 To Wscript.Arguments.Count - 1
      prgArgs = prgArgs & " " & chr(34) & Wscript.Arguments.Item(i) & chr(34)
   Next
End If

If Not WScript.Arguments.Named.Exists("elevate") Then
  CreateObject("Shell.Application").ShellExecute WScript.FullName _
    , chr(34) & WScript.ScriptFullName  & chr(34) & " " & prgArgs & " " & chr(34) & "/elevate" & chr(34), "", "runas", 1
  WScript.Quit
End If
WScript.Echo "Got: " & WScript.Arguments.Count & " params"

If  ( (fso.FolderExists(WScript.Arguments(0))) AND (fso.FolderExists( WScript.Arguments(1) )) AND (("false" = LCase(WScript.Arguments(2)) ) Or ( "true" = LCase(WScript.Arguments(2)) ))) Then
	WScript.Echo "Starting Platform Dependent Updater"
	WScript.Echo "Waiting 3 sec"
	WScript.Sleep 3000
	Dim oShell
	Set oShell = WScript.CreateObject ("WScript.Shell")
	oShell.Run "taskkill /f /im ""java.exe""", , True
	WScript.Echo "Waiting 3 sec"
	WScript.Sleep 3000
	oShell.Run "taskkill /f /im ""java.exe""", , True
rem	WScript.Echo "remove_jre.bat" & " " & chr(34) & Wscript.Arguments(0) & chr(34)
	oShell.CurrentDirectory = WScript.Arguments(1)
rem	oShell.Run "install_jre.bat" & " " & chr(34) & Wscript.Arguments(0) & chr(34), 1, True

	WScript.Echo "Copy update files"

	Set fso = CreateObject("Scripting.FileSystemObject")
	Set objFolder = fso.GetFolder(WScript.Arguments(1))

	Wscript.Echo objFolder.Path

	Set colFiles = objFolder.Files
	Wscript.Echo "Copy root files"
	For Each objFile in colFiles
		targetFilePath = Wscript.Arguments(0) & "\" & objFile.Name
		isReadonly = MakeReadWrite(targetFilePath)
		fso.CopyFile objFile.Path, Wscript.Arguments(0) & "\", True
		if (isReadonly) then
			MakeReadonly(targetFilePath)
		End If
    Next
	Wscript.Echo "Root files were copied. Copy subfolders..."
	CopySubfolders fso.GetFolder(objFolder)
	Wscript.Echo "Subfolders were copied"
	Set objShell = Wscript.CreateObject("WScript.Shell")
	objShell.CurrentDirectory = WScript.Arguments(0)
    	objShell.Run "make_transport_shortcut.vbs" & " " & chr(34) & WScript.Arguments(0) & chr(34) & "\.."
    if  ("true" = LCase(WScript.Arguments(2))) Then
        WScript.Echo "Start desktop application"
 	objShell.Run chr(34) & WScript.Arguments(0) & "\start-desktop.vbs" & chr(34)
    else
        WScript.Echo "Start command line application"
        objShell.Run chr(34) & WScript.Arguments(0) & "\start.vbs" & chr(34)
    End If
	WScript.Echo "Exit"
Else
	WScript.Echo "Invalid input parameters:" & WScript.Arguments(0) & " " & WScript.Arguments(1) & " " & WScript.Arguments(2)
End If

Sub CopySubFolders(Folder)
    For Each Subfolder in Folder.SubFolders
		targetFolderPath = Replace(LCase(SubFolder.Path), LCase(WScript.Arguments(1)), WScript.Arguments(0))
		WScript.Echo targetFolderPath	
		if (Not fso.FolderExists(targetFolderPath)) then
			fso.CreateFolder targetFolderPath
		End If

        Set objFolder = fso.GetFolder(Subfolder.Path)

        Set colFiles = objFolder.Files

        For Each objFile in colFiles
			targetFilePath = targetFolderPath & "\" & objFile.Name
			Wscript.Echo targetFilePath
			isReadonly = MakeReadWrite(targetFilePath)
			fso.CopyFile objFile.Path, targetFolderPath & "\", True
			if (isReadonly) then
				MakeReadonly(targetFilePath)
			End If
        Next

        CopySubFolders Subfolder

    Next

End Sub

Function MakeReadWrite(File)
    		MakeReadWrite = false
			if (fso.FileExists(File)) then
				Set f = fso.GetFile(File)
				If f.Attributes AND ReadOnly Then
					f.Attributes = f.Attributes XOR ReadOnly
					MakeReadWrite = true
				End If
			End If
End Function
Function MakeReadonly(File)
    		MakeReadonly = false
			if (fso.FileExists(File)) then
				Set f = fso.GetFile(File)
				If Not(f.Attributes AND ReadOnly) Then
					f.Attributes = f.Attributes XOR ReadOnly
					MakeReadonly = true
				End If
			End If
End Function
