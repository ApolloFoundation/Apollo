package com.apollocurrency.aplwallet.apl.conf;

import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Version;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class ConfigVerifierTest {

//    ConfigVerifier configVerifier;

    @AfterEach
    void tearDown() {
//        ConfigVerifier configVerifier = null;
    }

    @SneakyThrows
    @Test
    void test_createEmpty() {
        ConfigVerifier configVerifier = ConfigVerifier.create(null);
        assertNotNull(configVerifier);
        assertEquals(162, configVerifier.getKnownProps().size());
    }

    @SneakyThrows
    @Test
    void test_createNyKnownPath() {
        ConfigVerifier configVerifier = ConfigVerifier.create("conf-tn2/apl-blockchain.properties");
        assertNotNull(configVerifier);
        assertEquals(162, configVerifier.getKnownProps().size());
    }

    @SneakyThrows
    @Test
    void test_parse() {
        ConfigVerifier configVerifier = ConfigVerifier.create("conf-tn2/apl-blockchain.properties");
        assertNotNull(configVerifier);
        assertEquals(162, configVerifier.getKnownProps().size());

        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
            null,
            false,
            null,
            Constants.APPLICATION_DIR_NAME + ".properties",
            Collections.emptyList());
        Properties applicationProperties = propertiesLoader.load();
        Properties properties = configVerifier.parse(applicationProperties, new Version(1, 25, 1));
        assertEquals(159, properties.size());
    }

    @SneakyThrows
    @Test
    void dumpToProperties() {
        ConfigVerifier configVerifier = ConfigVerifier.create("conf-tn2/apl-blockchain.properties");
        assertNotNull(configVerifier);
        assertEquals(162, configVerifier.getKnownProps().size());
        configVerifier.dumpToProperties(System.out);
    }

    @SneakyThrows
    @Test
    void test_isSupported() {
        ConfigVerifier configVerifier = ConfigVerifier.create("conf-tn1/apl-blockchain.properties");
        assertNotNull(configVerifier);
        assertEquals(163, configVerifier.getKnownProps().size());
        assertTrue(configVerifier.isSupported("apl.myHallmark"));
    }

    @SneakyThrows
    @Test
    void test_get() {
        ConfigVerifier configVerifier = ConfigVerifier.create("conf-tn1/apl-blockchain.properties");
        assertNotNull(configVerifier);
        assertEquals(163, configVerifier.getKnownProps().size());
        assertNotNull(configVerifier.get("apl.myHallmark"));
        assertNull(configVerifier.get("apl.unknownPropertyName"));
    }

    @SneakyThrows
    @Test
    void test_listDeprecated() {
        ConfigVerifier configVerifier = ConfigVerifier.create("conf/apl-blockchain.properties");
        assertNotNull(configVerifier);
        assertEquals(162, configVerifier.getKnownProps().size());
        List<ConfigRecord> list = configVerifier.listDeprecated(new Version(1, 25, 3));
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(new Version(1, 25, 1), list.get(0).deprecatedSince);
    }

    @SneakyThrows
    @Test
    void test_listNewAfter() {
        ConfigVerifier configVerifier = ConfigVerifier.create("conf/apl-blockchain.properties");
        assertNotNull(configVerifier);
        assertEquals(162, configVerifier.getKnownProps().size());
        List<ConfigRecord> list = configVerifier.listNewAfter(new Version(1, 35, 1));
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals(new Version(1, 48, 0), list.get(0).sinceRelease);
    }
}