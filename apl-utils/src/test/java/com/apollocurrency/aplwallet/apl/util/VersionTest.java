package com.apollocurrency.aplwallet.apl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VersionTest {

    @Test
    @DisplayName("Create incorrect version")
    void createIncorrectOne() {
        Version one = new Version(-10, -200, -3000);
        assertNotNull(one);
        assertEquals(-10, one.getMajorVersion());
        assertEquals(-200, one.getIntermediateVersion());
        assertEquals(-3000, one.getMinorVersion());

        one = new Version(1, 2, 3);
        assertNotNull(one);
    }

    @Test
    @DisplayName("Can't parse incorrect versions.")
    void parseOne() {
        assertThrows(NullPointerException.class, () -> new Version(null));
        assertThrows(RuntimeException.class, () -> new Version(""));
        assertThrows(RuntimeException.class, () -> new Version("-1.-1.-1"));
        assertThrows(RuntimeException.class, () -> new Version("-1.-1.0"));
        assertThrows(RuntimeException.class, () -> new Version("-1.0.0"));
        assertThrows(RuntimeException.class, () -> new Version("-.0.0"));
        assertThrows(RuntimeException.class, () -> new Version("0.0.%"));
        assertThrows(RuntimeException.class, () -> new Version("E.0.5"));
        assertThrows(RuntimeException.class, () -> new Version("0.0.5.00"));
        assertThrows(RuntimeException.class, () -> new Version("0.0.5.00.4"));
        assertThrows(RuntimeException.class, () -> new Version("0.0"));
        assertThrows(RuntimeException.class, () -> new Version("0"));
        assertThrows(RuntimeException.class, () -> new Version("..."));
    }

    @Test
    void fromOk() {
        Version one = new Version("1.2.3");
        assertNotNull(one);
        assertEquals(1, one.getMajorVersion());
        assertEquals(2, one.getIntermediateVersion());
        assertEquals(3, one.getMinorVersion());
    }

}