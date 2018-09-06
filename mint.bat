rem @ECHO OFF
if exist jre ( 
    set javaDir=jre\bin\
)


%javaDir%java.exe -cp target\classes;target\lib\*;conf com.apollocurrency.aplwallet.apl.mint.MintWorker