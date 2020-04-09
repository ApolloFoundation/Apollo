/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env;

import org.apache.commons.lang3.SystemUtils;

public enum Architecture {
    X86(0), AMD64(1), ARM(2);

    public final byte code;

    Architecture(int code) {
        this.code = (byte) code;
    }

    public static Architecture current() {
        String osArch = SystemUtils.OS_ARCH.toLowerCase();
        switch (osArch) {
            case "x86_64":
            case "amd64" :
                return AMD64;
            case "x86" :
                return X86;
        }
        if (osArch.startsWith("arm") || osArch.startsWith("aarch64")) {
            return ARM;
        }
        return null;
    }

    public static Architecture from(int code) {
        for (Architecture value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find architecture for code " + code);
    }
}
