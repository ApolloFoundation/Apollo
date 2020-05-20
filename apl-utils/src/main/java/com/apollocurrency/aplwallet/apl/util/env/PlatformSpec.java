package com.apollocurrency.aplwallet.apl.util.env;

import java.util.Objects;

public class PlatformSpec {
    private final OS os;
    private final Arch architecture;

    public PlatformSpec(OS os, Arch architecture) {
        this.os = os;
        this.architecture = architecture;
    }

    public static PlatformSpec current() {
        return new PlatformSpec(OS.current(), Arch.current());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlatformSpec that = (PlatformSpec) o;
        return os == that.os &&
            architecture == that.architecture;
    }

    @Override
    public int hashCode() {
        return Objects.hash(os, architecture);
    }

    public OS getOS() {
        return os;
    }

    public Arch getArchitecture() {
        return architecture;
    }
}


