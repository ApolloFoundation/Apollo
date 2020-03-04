/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.udpater.intfce;

import lombok.Getter;

@Getter
public enum Level {
    CRITICAL(0), IMPORTANT(1), MINOR(2);

    Level(int code) {
        this.code = (byte) code;
    }

    private final byte code;

    public static Level from(int code) {
        for (Level value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unable to find appropriate Level for code " + code);
    }
}
