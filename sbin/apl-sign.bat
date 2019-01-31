@REM Sign transaction offline and manually
@echo *********************************************************************
@echo * This shell file will sign transaction json stored in file and     *
@echo * write signed transaction to output file. Works in combined mode   *
@echo * Read secretPhrase(standard wallet) or secretKeyBytes(vault wallet)*
@echo * from console also cmd parameters required                         *
@echo * Cmd parameters (order is important)                               *
@echo * a) Unsigned transaction file path - mandatory                     *
@echo * a) Signed transaction file path - optional                        *
@echo * TODO fix app launch                                               *
@echo *********************************************************************
if exist jre (
    set javaDir=jre\bin\
)

%javaDir%java.exe -Xmx1024m -cp "target/classes;target/lib/*;conf" -Dapl.runtime.mode=user com.apollocurrency.aplwallet.apl.tools.SignTransactionJSON
