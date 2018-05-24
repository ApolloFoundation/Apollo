package test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

public class TestUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private TestUtil() {} //never

    public static URI createURI(String url) {
        try {
            return new URI(url);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e); //re-throw unchecked
        }
    }

    public static Long nqt(long amount) {
        return 100_000_000L * amount;
    }

    public static ObjectMapper getMAPPER() {
        return MAPPER;
    }

    public static Map<String, String> loadKeys(String fileName) {
        Map<String, String> accounts = new HashMap<>();
        Properties keys = new Properties();
        try {
            keys.load(new BufferedReader(new FileReader(new File(fileName))));
            keys.forEach((rs, pk) -> accounts.put((String) rs, (String) pk));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return accounts;
    }
}
