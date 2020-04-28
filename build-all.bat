@echo off
@REM common functions for Apollo scrtips
@echo *********** BUILD apl-bom-ext **********
cd apl-bom-ext
call mvnw clean install

@echo *********** BUILD apollo-wallet **********
cd ..
call mvnw clean install

@echo *********** BUILD apl-bom **********
cd apl-bom
call mvnw clean install
