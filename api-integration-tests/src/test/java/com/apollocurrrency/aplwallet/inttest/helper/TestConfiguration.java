package com.apollocurrrency.aplwallet.inttest.helper;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;

public class TestConfiguration {
    private JSONParser parser;
    private static TestConfiguration testConfig;
    private String host;
    private String port;
    private String user;
    private String pass;
    private String userVault;
    private String passVault;
    private String publicKey;
    private String adminPass;

    private TestConfiguration(){
        try {
            parser = new JSONParser();
            Object obj = parser.parse(new FileReader("src\\test\\resources\\config.json"));
            JSONObject jsonObject = (JSONObject) obj;
            host = (String) jsonObject.get("host");
            port = (String) jsonObject.get("port");
            user = (String) jsonObject.get("user");
            pass = (String) jsonObject.get("pass");
            userVault = (String) jsonObject.get("userVault");
            passVault = (String) jsonObject.get("passVault");
            publicKey = (String) jsonObject.get("publicKey");
            adminPass = (String) jsonObject.get("adminPassword");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static TestConfiguration getTestConfiguration() {
        if (testConfig == null){
            testConfig = new TestConfiguration();
        }
        return testConfig;
    }

    public String getBaseURL() {
        return host;
    }
    public String getPort() {
        return port;
    }
    public String getTestUser() {
            return user;
    }
    public String getSecretPhrase() {
        return pass;
    }
    public String getPublicKey() {
        return publicKey;
    }
    public String getUserVault() {
        return userVault;
    }
    public String getPassVault() {
        return passVault;
    }
    public String getAdminPass() {
        return adminPass;
    }
}
