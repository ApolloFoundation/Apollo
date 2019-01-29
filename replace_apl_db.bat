@echo off
setlocal EnableExtensions EnableDelayedExpansion
set "INTEXTFILE=conf\apl-blockchain.properties"
set "OUTTEXTFILE=conf\apl-blockchain.properties_"
set "OUTTEXTFILE2=conf\apl-blockchain.properties__"
set "SEARCHTEXT=./apl_db/apl"
set "REPLACETEXT=./apl_db"
set "SEARCHTEXT2=./apl_test_db/apl"
set "REPLACETEXT2=./apl_test_db"

for /f "delims=" %%A in ('type "%INTEXTFILE%"') do (
    set "string=%%A"
    set "modified=!string:%SEARCHTEXT%=%REPLACETEXT%!"
    echo !modified!>>"%OUTTEXTFILE%"
)

for /f "delims=" %%A in ('type "%OUTTEXTFILE%"') do (
    set "string=%%A"
    set "modified=!string:%SEARCHTEXT2%=%REPLACETEXT2%!"
    echo !modified!>>"%OUTTEXTFILE2%"
)

del "%INTEXTFILE%"
copy "%OUTTEXTFILE2%" "%INTEXTFILE%"
del "%OUTTEXTFILE%"
del "%OUTTEXTFILE%2"
endlocal
