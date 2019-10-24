package com.apollocurrrency.aplwallet.inttest.helper;


import com.apollocurrrency.aplwallet.inttest.model.NetConfig;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.mapper;


public class TestConfiguration {
    private JSONParser parser;
    private static TestConfiguration testConfig;
    private String host;
    private String port;
    private Wallet standartWallet;
    private Wallet vaultWallet;
    private String adminPass;
    private List<String> hosts;
    private String env;

    private TestConfiguration(){
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            parser = new JSONParser();
            Object obj = parser.parse(new FileReader(classLoader.getResource("config.json").getFile()));
            JSONObject jsonObject = (JSONObject) obj;
            host = (String) jsonObject.get("host");
            port = (String) jsonObject.get("port");
            adminPass = (String) jsonObject.get("adminPassword");
            standartWallet = mapper.readValue( jsonObject.get("standartWallet").toString(), Wallet.class);
            vaultWallet= mapper.readValue(jsonObject.get("vaultWallet").toString(), Wallet.class);
            HashMap<String, NetConfig> testNetIp = mapper.readValue(jsonObject.get("net").toString(), HashMap.class);
            Random rand = new Random();
            env = System.getProperty("test.env");
            if (!env.equals(host)){
                hosts = testNetIp.get(env).getPeers();
                host = hosts.get(rand.nextInt(hosts.size()));
            }else {
                hosts = new ArrayList<>();
                hosts.add(host);
            }

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
    public List<String> getHosts() {
        return hosts;
    }
}
