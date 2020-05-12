/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

public enum Level {
    CRITICAL(0), IMPORTANT(1), MINOR(2);

    public final byte code;

    Level(int code) {
        this.code = (byte) code;
    }

    public static Level from(int code) {
        for (Level value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find appropriate Level for code " + code);
    }
}
