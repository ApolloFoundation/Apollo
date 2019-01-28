if exist jre ( 
    set javaDir=jre\bin\
)

%javaDir%java.exe -Xmx1024m -cp "target/classes;target/lib/*;conf" -Dapl.runtime.mode=desktop com.apollocurrency.aplwallet.apl.tools.SignTransactionJSON
