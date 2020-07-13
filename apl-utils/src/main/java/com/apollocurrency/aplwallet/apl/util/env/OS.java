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
@JsonDeserialize(using = OS.Deserializer.class)
public enum OS {
    LINUX(0, "Linux", "LINUX"),
    WINDOWS(1, "Windows", "WINDOWS"),
    MAC_OS(2, "Darwin", "MAC_OS","Mac", "Mac OS"),
    OSX(3, "OS_X", "OSX", "OS X"), // left for backward compatibility with existing blockchain,
    NO_OS(-1, "NoOS", "ALL");

    public final byte code;
    private final List<String> aliases = new ArrayList<>();
    private final String compatibleName;

    /**
     * Create an OS instance for given code, name, aliases
     * @param code byte identifier to serialize/deserialize
     * @param name application-wide name for given OS, should be used for all public purposes
     * @param compatibleName os name used by old updater impl to serialize update transaction, left here for compatibility purposes only
     * @param optionalAliases os-specific aliases to define from java properties and other user-specific input
     */
    OS(int code, String name,String compatibleName, String... optionalAliases) {
        this.code = (byte) code;
        aliases.add(name);
        this.compatibleName = compatibleName;
        if (optionalAliases != null) {
            Collections.addAll(this.aliases, optionalAliases);
        }
    }

    static final String OS_VALUE = System.getProperty("os.name");

    public static OS current() {
        for (OS value : values()) {
            if (contains(value.aliases)) {
                if (value == NO_OS) { // should not happen
                    throw new IllegalStateException("Generic OS detected as current OS for '" + OS_VALUE + "'");
                }
                return value;
            }
        }
        throw new IllegalStateException("Unable to detect current OS for '" + OS_VALUE + "'");
    }

    private static boolean contains(List<String> aliases) {
        return StringUtils.containsIgnoreCase(OS_VALUE, aliases);
    }

    public static OS from(int code) {
        for (OS value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find platform for code '" + code + "'");
    }

    public static OS from(String str) {
        for (OS value : values()) {
            if (value.eq(str.trim())) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find OS for '" + str + "'");
    }

    public static OS fromCompatible(String s) {
        for (OS value : values()) {
            if (StringUtils.isNotBlank(value.compatibleName) && value.compatibleName.equalsIgnoreCase(s)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find compatible OS for '" + s + "'");
    }

    public boolean isAppropriate(OS os) {
        return this == os || os == NO_OS;
    }

    private boolean eq(String str) {
        for (String alias : aliases) {
            if (alias.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public String getCompatibleName() {
        return compatibleName;
    }

    @Override
    public String toString() {
        return aliases.get(0);
    }

    static class Deserializer extends FromStringDeserializer<OS> {
        protected Deserializer() {
            super(OS.class);
        }
        @Override
        protected OS _deserialize(String value, DeserializationContext ctxt) {
            return from(value);
        }
    }
}
