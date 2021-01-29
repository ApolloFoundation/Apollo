' This script is used by Updater for Windows to update application directory
' Update will not started, when user did not grant admin privileges to this script
const ReadOnly = 1
const TorShortcutName = "Apollo TOR IP masking.lnk"
const TransportShortcutName = "Apollo Secure Transport IP masking.lnk"
const ApplicationShortcutName = "Apollo Wallet.lnk"
const ApplicationShortcutName2 = "Apollo.lnk"
const ServiceScriptPath = "\apollo-blockchain\bin\apl-start-service.vbs"
const DesktopScriptPath = "\apollo-desktop\bin\apl-start-desktop.vbs"
const TorDesktopScriptPath = "\apollo-blockchain\bin\apl-start-tor.vbs"
const TransportDesktopScriptPath = "\apollo-blockchain\bin\apl-start-secure-transport.vbs"

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
	WScript.Echo "update.bat" & " " & chr(34) & Wscript.Arguments(0) & chr(34) & " " & chr(34) & Wscript.Arguments(1) & chr(34), 1, True
	Shell.Run "update.bat" & " " & chr(34) & Wscript.Arguments(0) & chr(34) & " " & chr(34) & Wscript.Arguments(1) & chr(34), 1, True
	
	Wscript.Echo "Subfolders were copied"
	Dim ParentDir
	ParentDir =  fso.GetParentFolderName(WScript.Arguments(0))
	Rem Shell.CurrentDirectory = Wscript.Arguments(0) & "\..\apollo-blockchain\bin"
	if  ("true" = LCase(WScript.Arguments(2))) Then
        
		u = UpdateShortcut(Shell, fso, TorDesktopScriptPath, TorShortcutName)
		u = UpdateShortcut(Shell, fso, TransportDesktopScriptPath, TransportShortcutName)
		u = UpdateShortcut(Shell, fso, DesktopScriptPath, ApplicationShortcutName)
		u = UpdateShortcut(Shell, fso, DesktopScriptPath, ApplicationShortcutName2)
		WScript.Echo "Start user mode application"
		app = chr(34) & ParentDir & DesktopScriptPath & chr(34)
		WScript.Echo app
		Shell.Run app
    	else
        	WScript.Echo "Start service mode application"
		Shell.Run chr(34) & WScript.Arguments(0) & ServiceScriptPath & chr(34)
    	End If
		WScript.Echo "Exit"
	Shell.Run "clean.bat" & " " & chr(34) & Wscript.Arguments(0) & chr(34)
Else
	WScript.Echo "Invalid input parameters:" & WScript.Arguments(0) & " " & WScript.Arguments(1) & " " & WScript.Arguments(2)
End If

Function UpdateShortcut(Shell, Fs, ScriptName, ShorcutName)	
	DesktopFolder = Shell.SpecialFolders("Desktop")
	ShortcutPath = DesktopFolder & "\" & ShorcutName
	PublicDesktop = Shell.ExpandEnvironmentStrings("%PUBLIC%") & "\Desktop"
	PublicShortcutPath = PublicDesktop & "\" & ShorcutName
	UpdateShortcut = false
	Shortcuts = Array(ShortcutPath, PublicShortcutPath)
	Wscript.Echo fso.GetParentFolderName(WScript.Arguments(0)) & ScriptName
	for each path in Shortcuts 
		if (Fs.FileExists(path)) then
			Set shortcut = Shell.CreateShortcut(path)
			shortcut.TargetPath = ParentDir & ScriptName
			shortcut.IconLocation = ParentDir & "\apollo-blockchain\favicon.ico"
			shortcut.WorkingDirectory = ParentDir & "\apollo-blockchain\bin"
			shortcut.WindowStyle = 4
			shortcut.Save
			WScript.Echo "Updated for " & path & " name = " & ShorcutName & " to " & ScriptName
			UpdateShortcut = true
		Else
			WScript.Echo "Shortcut " & path & " does not exist. Skip it."
		End if
	Next
	
End Function