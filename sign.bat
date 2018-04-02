if exist jre ( 
    set javaDir=jre\bin\
)

%javaDir%java.exe -Xmx1024m -cp "classes;lib/*;conf" -Dapl.runtime.mode=desktop apl.tools.SignTransactionJSON
