package com.apollocurrrency.aplwallet.inttest.helper;


import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.io.IOException;

import static com.apollocurrrency.aplwallet.inttest.helper.TestHelper.mapper;


public class TestConfiguration {
    private JSONParser parser;
    private static TestConfiguration testConfig;
    private String host;
    private String port;
    private Wallet standartWallet;
    private Wallet vaultWallet;
    private String adminPass;

    private TestConfiguration(){
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            parser = new JSONParser();
           // Object obj = parser.parse(new FileReader("src\\test\\resources\\config.json"));
            Object obj = parser.parse(new FileReader(classLoader.getResource("config.json").getFile()));
            JSONObject jsonObject = (JSONObject) obj;
            host = (String) jsonObject.get("host");
            port = (String) jsonObject.get("port");
            adminPass = (String) jsonObject.get("adminPassword");
            standartWallet= mapper.readValue(jsonObject.get("standartWallet").toString(), Wallet.class);
            vaultWallet= mapper.readValue(jsonObject.get("vaultWallet").toString(), Wallet.class);


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
    public String getAdminPass() {
        return adminPass;
    }
    public Wallet getStandartWallet() {
        return standartWallet;
    }
    public Wallet getVaultWallet() {
        return vaultWallet;
    }
}
