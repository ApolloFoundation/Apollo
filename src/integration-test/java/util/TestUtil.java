/*
 * Copyright © 2018 Apollo Foundation
 */

package util;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.apollocurrency.aplwallet.apl.TransactionDeserializer;
import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dto.JSONTransaction;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Assert;
import org.slf4j.Logger;

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

    private TestUtil() {
    } //never


    public static  <T> void checkList(List<T> list) {
        Assert.assertNotNull(list);
        Assert.assertFalse(list.isEmpty());
    }

    public static void verifyOwner(List<JSONTransaction> transactions, BasicAccount account) {
        transactions.forEach(transaction -> {
            if (!transaction.isOwnedBy(account)) {
                Assert.fail(transaction.toString() + " is not for this address \'" + account + "\'");
            }
        });
    }
    public static void verifyOwner(List<JSONTransaction> transactions, String account) {
       verifyOwner(transactions, new BasicAccount(account));
    }


    public static URI createURI(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
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

    public static String getRandomRS(Map<String, String> accounts) {
        return new ArrayList<>(accounts.keySet()).get(RANDOM.nextInt(accounts.size()));
    }

    public static String getRandomSecretPhrase(Map<String, String> accounts) {
        return accounts.get(getRandomRS(accounts));
    }

    public static String getRandomRecipientRS(Map<String, String> accounts, String senderRS) {
        return new ArrayList<>(accounts.keySet()).stream().filter(rs -> !senderRS.equalsIgnoreCase(rs)).collect(Collectors.toList()).get(RANDOM.nextInt(accounts.size() - 1));
    }

    public static void deleteDir(Path dir, Predicate<Path> deleteFilter) throws IOException {
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (deleteFilter.test(file)) {
                    Files.delete(file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (deleteFilter.test(dir)) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteInKeystore(List<String> accounts) throws IOException {
        deleteDir(Apl.getKeystoreDir(Apl.getStringProperty("apl.testnetKeystoreDir")),
                (path -> accounts.stream().anyMatch(acc -> path.getFileName().toString().contains(acc))));
    }

    public static Matcher createStringMatcher(Object object) {
        return new BaseMatcher<Object>() {
            @Override
            public boolean matches(Object item) {
                return item.toString().contains(String.valueOf(object));
            }

            @Override
            public void describeTo(Description description) {

            }
        };
    }

    public static void verifyJsonAccount(String json, String account, String jsonNode) {
        JsonFluentAssert.assertThatJson(json)
                .node(jsonNode)
                .isPresent()
                .isStringEqualTo(account);
    }
    public static void verifyJsonAccountRS(String json, String account) {
        verifyJsonAccount(json, account, "accountRS");
    }
    public static void verifyJsonAccountId(String json, long accountId) {
        verifyJsonAccountRS(json, Convert.rsAccount(accountId));
    }

    public static void verifyJsonNodeContains(String json, Object object, String node) {

        JsonFluentAssert.assertThatJson(json)
                .node(node)
                .isPresent()
                .matches(TestUtil.createStringMatcher(object));
    }

    public static void verifyErrorDescriptionJsonNodeContains(String json, Object object) {
        verifyJsonNodeContains(json, object, "errorDescription");
    }
}
