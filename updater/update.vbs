' This script is used by Updater for Windows to update application directory
' Update will not started, when user did not grant admin privileges to this script
const ReadOnly = 1
const TorShortcutName = "Apollo TOR IP masking.lnk"
const TransportShortcutName = "Apollo Secure Transport IP masking.lnk"
const ApplicationShortcutName = "Apollo Wallet.lnk"
const ServiceScriptPath = "\bin\apl-start-service.vbs"
const DesktopScriptPath = "\bin\apl-start-desktop.vbs"
const TorDesktopScriptPath = "\bin\apl-start-tor.vbs"
const TransportDesktopScriptPath = "\bin\apl-start-secure-transport.vbs"

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

Set fso = CreateObject("Scripting.FileSystemObject")

If  ( (fso.FolderExists(WScript.Arguments(0))) AND (fso.FolderExists( WScript.Arguments(1) )) AND (("false" = LCase(WScript.Arguments(2)) ) Or ( "true" = LCase(WScript.Arguments(2)) ))) Then
	WScript.Echo "Starting Platform Dependent Updater"
	WScript.Echo "Waiting 3 sec"
	WScript.Sleep 3000
	Dim Shell
	Set Shell = WScript.CreateObject ("WScript.Shell")
	Shell.Run "taskkill /f /im ""java.exe""", , True
	WScript.Echo "Waiting 3 sec"
	WScript.Sleep 3000
	Shell.Run "taskkill /f /im ""java.exe""", , True
rem	WScript.Echo "remove_jre.bat" & " " & chr(34) & Wscript.Arguments(0) & chr(34)
	Shell.CurrentDirectory = WScript.Arguments(1)
	Shell.Run "install_libs.bat" & " " & chr(34) & Wscript.Arguments(0) & chr(34) & " " & Wscript.Arguments(4), 1, True

	WScript.Echo "Copy update files"

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
	Shell.CurrentDirectory = WScript.Arguments(0) & "\bin"
	if  ("true" = LCase(WScript.Arguments(2))) Then
        
		u = UpdateShortcut(Shell, fso, TorDesktopScriptPath, TorShortcutName)
		u = UpdateShortcut(Shell, fso, TransportDesktopScriptPath, TransportShortcutName)
		u = UpdateShortcut(Shell, fso, DesktopScriptPath, ApplicationShortcutName)
		WScript.Echo "Start user mode application"
		app = chr(34) & WScript.Arguments(0) & DesktopScriptPath & chr(34)
		WScript.Echo app
		Shell.Run app
    else
        WScript.Echo "Start service mode application"
		Shell.Run chr(34) & WScript.Arguments(0) & ServiceScriptPath & chr(34)
    End If
	WScript.Echo "Exit"
Else
	WScript.Echo "Invalid input parameters:" & WScript.Arguments(0) & " " & WScript.Arguments(1) & " " & WScript.Arguments(2)
End If



Sub CopySubFolders(Folder)
    For Each Subfolder in Folder.SubFolders
		targetFolderPath = Replace(LCase(SubFolder.Path), LCase(WScript.Arguments(1)), WScript.Arguments(0))
		rem WScript.Echo targetFolderPath	
		if (Not fso.FolderExists(targetFolderPath)) then
			fso.CreateFolder targetFolderPath
		End If

        Set objFolder = fso.GetFolder(Subfolder.Path)

        Set colFiles = objFolder.Files

        For Each objFile in colFiles
			targetFilePath = targetFolderPath & "\" & objFile.Name
			rem Wscript.Echo targetFilePath
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


Function UpdateShortcut(Shell, Fs, ScriptName, ShorcutName)	
	DesktopFolder = Shell.SpecialFolders("Desktop")
	ShortcutPath = DesktopFolder & "\" & ShorcutName
	PublicDesktop = Shell.ExpandEnvironmentStrings("%PUBLIC%") & "\Desktop"
	PublicShortcutPath = PublicDesktop & "\" & ShorcutName
	UpdateShortcut = false
	Shortcuts = Array(ShortcutPath, PublicShortcutPath)
	for each path in Shortcuts 
		if (Fs.FileExists(path)) then
			Set shortcut = Shell.CreateShortcut(path)
			shortcut.TargetPath = WScript.Arguments(0) & ScriptName
			shortcut.IconLocation = WScript.Arguments(0) & "\favicon.ico"
			shortcut.WorkingDirectory = WScript.Arguments(0) & "\bin"
			shortcut.WindowStyle = 4
			shortcut.Save
			WScript.Echo "Updated for " & path & " name = " & ShorcutName & " to " & ScriptName
			UpdateShortcut = true
		Else
			WScript.Echo "Shortcut " & path & " does not exist. Skip it."
		End if
	Next
	
End Function