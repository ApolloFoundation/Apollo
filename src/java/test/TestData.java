package test;

import java.util.ArrayList;
import java.util.List;

public class TestData {
    public static final List<String> URLS = new ArrayList<>();

    static {
        URLS.add("http://13.58.9.219:6876/apl");
        URLS.add("http://18.220.52.237:6876/apl");
        URLS.add("http://18.221.83.227:6876/apl");
        URLS.add("http://18.219.18.161:6876/apl");
        URLS.add("http://18.217.169.232:6876/apl");
    }

    public static final String TEST_FILE = "testnet-keys.properties";
    public static final String TEST_LOCALHOST = "http://127.0.0.1:6876/apl";
    public static final String MAIN_FILE = "mainnet-keys.properties";
    public static final String MAIN_LOCALHOST = "http://127.0.0.1:7876/apl";
}
