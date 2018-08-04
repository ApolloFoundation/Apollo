/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package test;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static test.TestData.URLS;

public class TestUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Random RANDOM = new Random();

    static {
        MAPPER.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private TestUtil() {
    } //never

    public static URI createURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e); //re-throw unchecked
        }
    }

    public static Long atm(long amount) {
        return 100_000_000L * amount;
    }

    public static Double fromATM(long amount) {
        return amount / (double) 100_000_000L;
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return accounts;
    }

    public static String randomUrl() {
        return URLS.get(RANDOM.nextInt(URLS.size()));
    }

    public static String getRandomRS(Map<String, String> accounts) {
        return new ArrayList<>(accounts.keySet()).get(RANDOM.nextInt(accounts.size()));
    }

    public static String getRandomSecretPhrase(Map<String, String> accounts) {
        return accounts.get(getRandomRS(accounts));
    }

    public static String getRandomRecipientRS(Map<String, String> accounts, String senderRS) {
        return new ArrayList<>(accounts.keySet()).stream().filter(rs -> !senderRS.equalsIgnoreCase(rs)).collect(Collectors.toList()).get(RANDOM.nextInt(accounts.size() - 1));
    }
}
