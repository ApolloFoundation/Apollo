package util;/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

public class TestUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Random RANDOM = new Random();
    private static final Logger LOG = getLogger(TestUtil.class);

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
        }
        catch (Exception e) {
            LOG.error("Cannot load keys!");
        }
        return accounts;
    }

    public static String randomUrl(List<String> urls) {
        return urls.get(RANDOM.nextInt(urls.size()));
    }

    public static String getRandomRS(Map<String,String> accounts) {
        return new ArrayList<>(accounts.keySet()).get(RANDOM.nextInt(accounts.size()));
    }

    public static String getRandomSecretPhrase(Map<String, String> accounts) {
        return accounts.get(getRandomRS(accounts));
    }

    public static String getRandomRecipientRS(Map<String,String> accounts, String senderRS) {
        return new ArrayList<>(accounts.keySet()).stream().filter(rs -> !senderRS.equalsIgnoreCase(rs)).collect(Collectors.toList()).get(RANDOM.nextInt(accounts.size() - 1));
    }
}
