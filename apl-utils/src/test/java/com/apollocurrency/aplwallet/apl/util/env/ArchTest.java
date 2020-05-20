package com.apollocurrency.aplwallet.apl.util.env;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
@Slf4j
class ArchTest {
    @Test
    void testCompatibility() {
        testCompatibility(Arch.X86_64, "AMD64", 1);
        testCompatibility(Arch.X86_32, "X86", 0);
        testCompatibility(Arch.ARM_64, "ARM", 2);
    }

    void testCompatibility(Arch arch, String compatibleName, int code) {
        assertEquals(arch, Arch.fromCompatible(compatibleName));
        assertEquals(arch.getCompatibleName(), compatibleName);
        assertEquals(arch.code, code);
    }

    @Test
    void testCurrentArch() {
        Arch current = Arch.current();
        log.info("Current arch: {}", current);
        assertNotEquals(Arch.ALL, current);
    }

    @Test
    void serializeDeserialize() throws JsonProcessingException {
        for (Arch value : Arch.values()) {
            String s = JSON.getMapper().writeValueAsString(value);
            assertEquals("\"" + value.toString() + "\"", s);
            Arch arch = JSON.getMapper().readValue(s, Arch.class);
            assertEquals(value, arch);
        }
    }

    @Test
    void testNewNames() {
        testNewName(" X86_64  ", Arch.X86_64);
        testNewName(" amd64  ", Arch.X86_64);
        testNewName(" X86  ", Arch.X86_32);
        testNewName("arm", Arch.ARM_32);
        testNewName("arm32", Arch.ARM_32);
        testNewName("aarch64", Arch.ARM_64);
        testNewName("arm64", Arch.ARM_64);
        testNewName("NoArch", Arch.ALL);

        testEx("x86_16");
        testEx("i386");
        testEx("ia32");
        testEx("ia64");
        testEx("ppc");
        testEx("powerpc");
        testEx("all");
    }

    void testEx(String name) {
        assertThrows(IllegalArgumentException.class, () -> Arch.from(name));
    }

    void testNewName(String name, Arch arch) {
        assertEquals(arch, Arch.from(name));
        assertEquals(arch, Arch.from(arch.toString()));
    }

}