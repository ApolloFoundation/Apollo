/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;


class OptionDAOTest {
    @RegisterExtension
    static DbExtension dbExtension = new DbExtension();

    private static OptionDAO optionDAO;

    @BeforeAll
    static void init() {
        optionDAO = new OptionDAO(dbExtension.getDatabaseManger());
    }

    @Test
    void get() {
        String value = optionDAO.get("unknown_key_1");
        assertNull(value);
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
    void setTwiceTheSameKey() {
        String key1 = "key1";
        boolean isInserted = optionDAO.set(key1, "value1");
        assertTrue(isInserted);
        String value = optionDAO.get(key1);
        assertNotNull(value);
        assertEquals("value1", value);

        isInserted = optionDAO.set(key1, "value2");
        assertTrue(isInserted);
        value = optionDAO.get(key1);
        assertEquals("value2", value);

        optionDAO.delete(key1);
        value = optionDAO.get(key1);
        assertNull(value);
    }

}