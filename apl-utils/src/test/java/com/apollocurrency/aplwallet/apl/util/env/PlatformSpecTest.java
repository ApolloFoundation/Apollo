package com.apollocurrency.aplwallet.apl.util.env;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlatformSpecTest {

    @Test
    void current() {
        PlatformSpec current = PlatformSpec.current();
        assertTrue(current.isAppropriate(new PlatformSpec(OS.NO_OS, Arch.NO_ARCH)));
    }

    @Test
    void isAppropriate() {
        PlatformSpec spec = new PlatformSpec(OS.WINDOWS, Arch.X86_64);
        assertTrue(spec.isAppropriate(new PlatformSpec(OS.NO_OS, Arch.NO_ARCH)));
        assertTrue(spec.isAppropriate(new PlatformSpec(OS.NO_OS, Arch.X86_64)));
        assertFalse(spec.isAppropriate(new PlatformSpec(OS.NO_OS, Arch.X86_32)));
        assertFalse(spec.isAppropriate(new PlatformSpec(OS.OSX, Arch.ARM_32)));
        assertTrue(spec.isAppropriate(new PlatformSpec(OS.WINDOWS, Arch.X86_64)));
        assertTrue(spec.isAppropriate(new PlatformSpec(OS.WINDOWS, Arch.NO_ARCH)));
        assertFalse(spec.isAppropriate(new PlatformSpec(OS.LINUX, Arch.NO_ARCH)));
    }
}