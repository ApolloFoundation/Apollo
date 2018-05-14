#  HOW TO LAUNCH TEST
1. Create file in root project directory with name *'testnet-keys.properties'*
2. Add to *'testnet-keys.properties'* few lines in format: *[your public apollo address]=[your secret phrase]*.  
For example: _APL-myAddress=secret phrase for this address_
3. Compile project using *compile.sh, win-compile.bat* or another script for your OS
4. Run *TestnetIntegrationScenario* Junit test using something like this:
```bat 
java -cp lib\*;classes;lib\junit-4.7.jar org.junit.runner.JUnitCore test.TestnetIntegrationScenario
```
5. Wait for test completement