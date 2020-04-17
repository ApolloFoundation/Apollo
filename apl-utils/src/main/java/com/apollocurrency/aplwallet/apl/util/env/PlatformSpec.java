package com.apollocurrency.aplwallet.apl.util.env;

import java.util.Objects;

public class PlatformSpec {
    private final Platform platform;
    private final Architecture architecture;

    public PlatformSpec(Platform platform, Architecture architecture) {
        this.platform = platform;
        this.architecture = architecture;
    }

    public static PlatformSpec current() {
        return new PlatformSpec(Platform.current(), Architecture.current());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlatformSpec that = (PlatformSpec) o;
        return platform == that.platform &&
            architecture == that.architecture;
    }

    @Override
    public int hashCode() {
        return Objects.hash(platform, architecture);
    }

    public Platform getPlatform() {
        return platform;
    }

    public Architecture getArchitecture() {
        return architecture;
    }
}


