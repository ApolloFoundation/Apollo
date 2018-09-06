@REM Attempt to recover mis-typed passphrase
@echo *********************************************************************
@echo * Use this batch file to search for a lost passphrase.              *
@echo *********************************************************************

if exist jre ( 
    set javaDir=jre\bin\
)


%javaDir%java.exe -Xmx1024m -cp "target/classes;target/lib/*;conf" -Dapl.runtime.mode=desktop com.apollocurrency.aplwallet.apl.tools.PassphraseRecovery
