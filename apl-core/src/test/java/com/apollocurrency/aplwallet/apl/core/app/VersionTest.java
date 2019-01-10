package com.apollocurrency.aplwallet.apl.core.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        one.setMajorVersion(-30);
        one.setIntermediateVersion(-70);
        one.setMinorVersion(-200);
        assertEquals(-30, one.getMajorVersion());
        assertEquals(-70, one.getIntermediateVersion());
        assertEquals(-200, one.getMinorVersion());
    }

    @Test
    @DisplayName("Can't parse incorrect versions.")
    void parseOne() {
        assertThrows(NullPointerException.class, () -> Version.from(null));
        assertThrows(RuntimeException.class, () -> Version.from(""));
        assertThrows(RuntimeException.class, () -> Version.from("-1.-1.-1"));
        assertThrows(RuntimeException.class, () -> Version.from("-1.-1.0"));
        assertThrows(RuntimeException.class, () -> Version.from("-1.0.0"));
        assertThrows(RuntimeException.class, () -> Version.from("-.0.0"));
        assertThrows(RuntimeException.class, () -> Version.from("0.0.%"));
        assertThrows(RuntimeException.class, () -> Version.from("E.0.5"));
        assertThrows(RuntimeException.class, () -> Version.from("0.0.5.00"));
        assertThrows(RuntimeException.class, () -> Version.from("0.0.5.00.4"));
        assertThrows(RuntimeException.class, () -> Version.from("0.0"));
        assertThrows(RuntimeException.class, () -> Version.from("0"));
        assertThrows(RuntimeException.class, () -> Version.from("..."));
    }

    @Test
    void fromOk() {
        Version one = Version.from("1.2.3");
        assertNotNull(one);
        assertEquals(1, one.getMajorVersion());
        assertEquals(2, one.getIntermediateVersion());
        assertEquals(3, one.getMinorVersion());
    }

}