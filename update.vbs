Set fso = CreateObject("Scripting.FileSystemObject")
const ReadOnly = 1
If Wscript.Arguments.Count > 1 Then
   For i = 0 To Wscript.Arguments.Count - 1
      prgArgs = prgArgs & " " & Wscript.Arguments.Item(i)
   Next
End If

If Not WScript.Arguments.Named.Exists("elevate") Then
  CreateObject("Shell.Application").ShellExecute WScript.FullName _
    , """" & WScript.ScriptFullName & Chr(34) & prgArgs & """ /elevate ", "", "runas", 1
  WScript.Quit
End If

If  ( (WScript.Arguments.Count = 5) AND (fso.FolderExists(WScript.Arguments(0))) AND (fso.FolderExists( WScript.Arguments(1) )) AND (("false" = LCase(WScript.Arguments(2)) ) Or ( "true" = LCase(WScript.Arguments(2)) ))) Then
	WScript.Echo "Starting Platform Dependent Updater"
	WScript.Echo "Waiting 3 sec"
	WScript.Sleep 3000
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
	if  ("true" = LCase(WScript.Arguments(2))) Then
        WScript.Echo "Start desktop application"
	    objShell.Run "start-desktop.vbs"
    else
        WScript.Echo "Start command line application"
        objShell.Run "start.vbs"
    End If
	WScript.Echo "Exit"
Else
	WScript.Echo "Invalid input parameters:" & WScript.Arguments(0) & " " & WScript.Arguments(1) & " " & WScript.Arguments(2)
End If


Sub CopySubFolders(Folder)
    For Each Subfolder in Folder.SubFolders
		targetFolderPath = Replace(SubFolder.Path, WScript.Arguments(1), WScript.Arguments(0))
		if (Not fso.FolderExists(targetFolderPath)) then
			fso.CreateFolder targetFolderPath
		End If

        Set objFolder = fso.GetFolder(Subfolder.Path)

        Set colFiles = objFolder.Files

        For Each objFile in colFiles
			targetFilePath = targetFolderPath & "\" & objFile.Name
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
