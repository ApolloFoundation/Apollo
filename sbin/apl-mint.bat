@REM Start mint worker
@echo ***********************************************************************
@echo * This batch file will start minting worker for mintable currencies   *
@echo * Take a look at "Mint" section in apl.properties for detailed config *
@echo ***********************************************************************
if exist jre ( 
    set javaDir=jre\bin\
)

%javaDir%java.exe -cp target\classes;target\lib\*;conf com.apollocurrency.aplwallet.apl.mint.MintWorker