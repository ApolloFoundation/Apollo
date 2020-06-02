package com.apollocurrency.aplwallet.apl.util.env;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.Objects;

@JsonSerialize(using = ToStringSerializer.class)
public class PlatformSpec {
    private final OS os;
    private final Arch arch;

    @JsonCreator
    public PlatformSpec(@JsonProperty("os") OS os, @JsonProperty("arch") Arch arch) {
        this.os = os;
        this.arch = arch;
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
            arch == that.arch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(os, arch);
    }

    public OS getOS() {
        return os;
    }

    public Arch getArch() {
        return arch;
    }

    @Override
    public String toString() {
        return os +
            "-" + arch;
    }

    public boolean isAppropriate(PlatformSpec other) {
        return os.isAppropriate(other.os) && arch.isAppropriate(other.arch);
    }
}


