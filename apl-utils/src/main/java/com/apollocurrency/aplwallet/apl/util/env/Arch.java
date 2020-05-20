/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env;

import com.apollocurrency.aplwallet.apl.util.StringUtils;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@JsonSerialize(using = ToStringSerializer.class)
@JsonDeserialize(using = Arch.Deserializer.class)
public enum Arch {
    X86_32(0, "X86",  "X86", "X86_32"),
    X86_64(1, "X86_64", "AMD64","Amd64"),
    ARM_64(2, "ARM64","ARM","aarch64"),
    ARM_32(3, "ARM32", null,"ARM"),
    ALL(4, "NoArch", null);

    public final byte code;
    private final List<String> aliases = new ArrayList<>();
    private final String compatibleName;
    /**
     * Create an architecture with given code, name and aliases
     * @param code byte identifier to serialize/deserialize to bytes
     * @param name application-wide name of this architecture, should be publicly used everywhere
     * @param compatibleName architecture name for compatibility purposes with old updater transactions, should be null
     *                      for new architectures, required for networks, which used old updater
     * @param optionalAliases optional aliases to detect this architecture from java properties or arbitrary input string
     */
    Arch(int code, String name, String compatibleName, String... optionalAliases) {
        this.code = (byte) code;
        this.compatibleName = compatibleName;
        aliases.add(name);
        if (optionalAliases != null) {
            Collections.addAll(this.aliases, optionalAliases);
        }
    }

    private static final String ARCH = System.getProperty("os.arch");
    public static Arch current() {
        for (Arch value : values()) {
            if (matches(value.aliases)) {
                if (value == ALL) {
                    throw new IllegalStateException("Unknown architecture mapped to generic type: " + ARCH);
                }
                return value;
            }
        }
        throw new IllegalStateException("Unknown architecture, unable to detect arch for: '" + ARCH + "'");
    }

    private static boolean matches(List<String> aliases) {
        return StringUtils.equalsIgnoreCase(ARCH, aliases);
    }

    public static Arch from(String str) {
        for (Arch value : values()) {
            if (StringUtils.equalsIgnoreCase(str, value.aliases)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find Architecture for '" + str + "'");
    }
    public static Arch fromCompatible(String str) {
        for (Arch value : values()) {
            String compatibleName = value.getCompatibleName();
            if (StringUtils.isNotBlank(compatibleName) && compatibleName.equalsIgnoreCase(str)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find compatible Architecture for '" + str + "'");
    }

    public static Arch from(int code) {
        for (Arch value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find architecture for code " + code);
    }

    public String getCompatibleName() {
        return compatibleName;
    }

    @Override
    public String toString() {
        return aliases.get(0); // return name
    }
    static class Deserializer extends FromStringDeserializer<Arch> {
        protected Deserializer() {
            super(Arch.class);
        }
        @Override
        protected Arch _deserialize(String value, DeserializationContext ctxt) {
            return from(value);
        }
    }
}
