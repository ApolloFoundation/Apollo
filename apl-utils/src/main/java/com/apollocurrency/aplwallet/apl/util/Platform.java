/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util;

import org.apache.commons.lang3.SystemUtils;

public enum Platform {

    LINUX(0), WINDOWS(1), MAC_OS(2), OSX(3), ALL(-1);
    public final byte code;

    Platform(int code) {
        this.code = (byte) code;
    }

    public static Platform current() {
        return
               SystemUtils.IS_OS_WINDOWS ? Platform.WINDOWS : // Windows
               SystemUtils.IS_OS_LINUX  ? Platform.LINUX   : // Linux
               SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX ? Platform.MAC_OS : // Mac
                                                                    null;              // Other
    }

    public boolean isAppropriate(Platform platform) {
        return this == platform || platform == ALL;
    }

    public static Platform from(int code) {
        for (Platform value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find platform for code " + code);
    }
}
