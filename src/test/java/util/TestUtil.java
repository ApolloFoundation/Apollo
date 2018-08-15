/*
 * Copyright © 2018 Apollo Foundation
 */

package util;

import com.apollocurrency.aplwallet.apl.JSONTransaction;
import com.apollocurrency.aplwallet.apl.TransactionDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.Assert;
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
        SimpleModule module = new SimpleModule();
        module.addDeserializer(JSONTransaction.class, new TransactionDeserializer());
        MAPPER.registerModule(module);
    }

    private TestUtil() {} //never


    public static  <T> void checkList(List<T> list) {
        Assert.assertNotNull(list);
        Assert.assertFalse(list.isEmpty());
    }

    public static void checkAddress(List<JSONTransaction> transactions, String address) {
        transactions.forEach(transaction -> {
            if (!transaction.getSenderRS().equalsIgnoreCase(address) && !transaction.getRecipientRS().equalsIgnoreCase(address)) {
                Assert.fail(transaction.toString() + " is not for this address \'" + address + "\'");
            }
        });
    }


    public static URI createURI(String url) {
        try {
            return new URI(url);
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e); //re-throw unchecked
        }
    }

    public static long atm(long amount) {
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
