package com.apollocurrency.aplwallet.apl.util.env;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class OSTest {
    @Test
    void testFromCompatible() {
        testCompatibility(OS.WINDOWS, "WINDOWS");
        testCompatibility(OS.LINUX, "LINUX");
        testCompatibility(OS.MAC_OS, "MAC_OS");
        testCompatibility(OS.NO_OS, "ALL");
        testCompatibility(OS.OSX, "OSX");
    }

    @Test
    void testCurrent() {
        OS current = OS.current();
        log.info("Current os detected: {}", current);
        assertNotEquals(current, OS.NO_OS);
    }

    @Test
    void testNewNameCode() {
        testNameAndCode(OS.WINDOWS, "Windows");
        testNameAndCode(OS.LINUX, "Linux");
        testNameAndCode(OS.MAC_OS, "Darwin");
        testNameAndCode(OS.NO_OS, "NoOS");
        testNameAndCode(OS.OSX, "OS_X");
    }

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        for (OS value : OS.values()) {
            String s = JSON.getMapper().writeValueAsString(value);
            assertEquals("\"" + value.toString() + "\"", s);
            OS os = JSON.getMapper().readValue(s, OS.class);
            assertEquals(value, os);
        }
    }

    @Test
    void testFrom() {
        testFrom(OS.WINDOWS, "  WiNdoWs");
        testFrom(OS.WINDOWS, "WiNdoWs");
        testFromEx("windows NT blah  ");
        testFromEx("win");
        testFromEx("window");
        testFrom(OS.LINUX, "  linux");
        testFrom(OS.LINUX, "LINUx");
        testFromEx("lin");
        testFromEx("unix");
        testFromEx("sunos");
        testFrom(OS.MAC_OS, "mac");
        testFrom(OS.MAC_OS, "darwin");
        testFrom(OS.MAC_OS, " Darwin ");
        testFrom(OS.MAC_OS, "Mac");
        testFrom(OS.MAC_OS, "Mac OS");
        testFromEx("macos");
        testFromEx("macosx");
        testFrom(OS.OSX, "os_x");
        testFrom(OS.OSX, "os x");
        testFrom(OS.NO_OS, "NoOS");
        testFromEx("All");
        testFromEx("all");
    }

    private void testFromEx(String str) {
        assertThrows(IllegalArgumentException.class, () -> OS.from(str));
    }

    void testFrom(OS os, String name) {
        OS actual = OS.from(name);
        assertEquals(os, actual);
    }

    void testNameAndCode(OS os, String name) {
        assertEquals(os.toString(), name);
        assertEquals(os, OS.from(os.code));
    }

    void testCompatibility(OS os, String oldName) {
        OS actual = OS.fromCompatible(oldName);
        assertEquals(os, actual);
        String compatibleName = os.getCompatibleName();
        assertEquals(oldName, compatibleName);
    }

}