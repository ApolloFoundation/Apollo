/*
 * Copyright © 2017-2018 Apollo Foundation
 */


package test;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.slf4j.LoggerFactory.getLogger;

public class TestData {
    public static final List<String> URLS = new ArrayList<>();
    private static final Logger LOG = getLogger(TestData.class);

    static {
        Properties aplProperties = new Properties();
        try {
            aplProperties.load(Files.newInputStream(Paths.get("conf/apl.properties")));
            String[] ips = aplProperties.getProperty("apl.defaultTestnetPeers").split(";");
            for (int i = 0; i < ips.length; i++) {
                URLS.add("http://" + ips[i] + ":6876/apl");
            }
        } catch (IOException e) {
            LOG.error("Cannot read ip's for peers from conf/apl.properties", e);
        }
    }

    public static final String TEST_FILE = "testnet-keys.properties";
    public static final String TEST_LOCALHOST = "http://127.0.0.1:6876/apl";
    public static final String MAIN_FILE = "mainnet-keys.properties";
    public static final String MAIN_LOCALHOST = "http://127.0.0.1:7876/apl";
    public static final String MAIN_RS = "APL-NZKH-MZRE-2CTT-98NPZ";
}
