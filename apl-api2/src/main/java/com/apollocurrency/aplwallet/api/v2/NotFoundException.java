/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.api.v2;

import lombok.Getter;

public class NotFoundException extends javax.ws.rs.NotFoundException {
    @Getter
    private final int code;
    public NotFoundException (int code, String msg) {
        super(msg);
        this.code = code;
    }
}
