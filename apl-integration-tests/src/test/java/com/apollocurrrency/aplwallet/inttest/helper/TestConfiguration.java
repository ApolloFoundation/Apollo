package com.apollocurrrency.aplwallet.inttest.helper;


import com.apollocurrrency.aplwallet.inttest.model.NetConfig;
import com.apollocurrrency.aplwallet.inttest.model.Wallet;
import com.fasterxml.jackson.core.type.TypeReference;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static com.apollocurrrency.aplwallet.inttest.helper.HttpHelper.mapper;


public class TestConfiguration {
    private static TestConfiguration testConfig;
    private JSONParser parser;
    private String host;
    private String port;
    private Wallet standartWallet;
    private Wallet genesisWallet;
    private Wallet vaultWallet;
    private File defaultImage;

    private String adminPass;
    private List<String> peers;
    private String env;
    private HashMap<String, NetConfig> testNetIp;


    private TestConfiguration() {
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            parser = new JSONParser();
            Object obj = parser.parse(new FileReader(classLoader.getResource("config.json").getFile()));
            JSONObject jsonObject = (JSONObject) obj;
            host = (String) jsonObject.get("host");
            port = (String) jsonObject.get("port");
            defaultImage = new File(classLoader.getResource(String.valueOf(jsonObject.get("defaultImage"))).getPath());
            adminPass = (String) jsonObject.get("adminPassword");
            standartWallet = mapper.readValue(jsonObject.get("standartWallet").toString(), Wallet.class);
            vaultWallet = mapper.readValue(jsonObject.get("vaultWallet").toString(), Wallet.class);
            genesisWallet = mapper.readValue(jsonObject.get("genesisWallet").toString(), Wallet.class);
            TypeReference<HashMap<String, NetConfig>> typeRef = new TypeReference<>() {
            };
            testNetIp = mapper.readValue(jsonObject.get("net").toString(), typeRef);
            Random rand = new Random();
            env = System.getProperty("test.env");
            if (!env.equals(host)) {
                peers = testNetIp.get(env).getPeers();
                host = peers.get(rand.nextInt(peers.size()));
            } else {
                peers = new ArrayList<>();
                peers.add(host);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TestConfiguration getTestConfiguration() {
        if (testConfig == null) {
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

    public List<String> getPeers() {
        return peers;
    }

    public List<String> getHostsByChainID(String chainId) {
        testNetIp.forEach((k, v) -> {
            if (v.getChainId().equals(chainId)) {
                this.peers = v.getPeers();
            }
        });
        return this.peers;
    }

    public Wallet getGenesisWallet() {
        return genesisWallet;
    }

    public File getDefaultImage() {
        return defaultImage;
    }
}
