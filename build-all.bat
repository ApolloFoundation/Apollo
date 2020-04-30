@echo off
@echo *********** BUILD Apollo **********
@echo ----------------------------------------
@echo *********** BUILD apl-bom-ext **********
cd apl-bom-ext
call mvnw clean install

@echo *********** BUILD apollo-wallet (including apl-bom) **********
cd ..
call mvnw clean install %1