/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.appdata;

import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import lombok.extern.slf4j.Slf4j;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@Tag("slow")
@Slf4j
@EnableWeld
class OptionDAOTest {

    @Container
    public static final GenericContainer mariaDBContainer = new MariaDBContainer("mariadb:10.5")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass")
        .withExposedPorts(3306)
        .withLogConsumer(new Slf4jLogConsumer(log));

    @RegisterExtension
    DbExtension extension = new DbExtension(mariaDBContainer);

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(OptionDAO.class)
        .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
        .build();


    @Inject
    private OptionDAO optionDAO;

    @Test
    void get() {
        String value = optionDAO.get("unknown_key_1");
        assertNull(value);
    }

    @Test
    void exist() {
        boolean exists = optionDAO.exist("unknown_key_1");
        assertFalse(exists);
        exists = optionDAO.exist("existingKey");
        assertTrue(exists);
        exists = optionDAO.exist("existingNullKey");
        assertTrue(exists);
        exists = optionDAO.exist("existingEmptyKey");
        assertTrue(exists);
    }

    @Test
    void set() {
        String unknown_key = "unknown_key_2";
        boolean isInserted = optionDAO.set(unknown_key, "unknown_value");
        assertTrue(isInserted);
        String value = optionDAO.get(unknown_key);
        assertNotNull(value);
        assertEquals("unknown_value", value);

        optionDAO.delete(unknown_key);
        value = optionDAO.get(unknown_key);
        assertNull(value);
    }

    @Test
    void setMultipleTimesSameKey() {
        String key1 = "key1";
        boolean isInserted = optionDAO.set(key1, "value1");
        assertTrue(isInserted);
        String value1 = optionDAO.get(key1); // put value1
        assertNotNull(value1);
        assertEquals("value1", value1);

        isInserted = optionDAO.set(key1, null); // put null
        assertTrue(isInserted);
        String value2 = optionDAO.get(key1);
        assertNull(value2);

        isInserted = optionDAO.set(key1, "value2"); // put another value2
        assertTrue(isInserted);
        String value3 = optionDAO.get(key1);
        assertEquals("value2", value3);

        isInserted = optionDAO.set(key1, ""); // put empty
        assertTrue(isInserted);
        String value4 = optionDAO.get(key1);
        assertEquals("", value4);

        optionDAO.delete(key1);
        String value5 = optionDAO.get(key1);
        assertNull(value5);
    }

}