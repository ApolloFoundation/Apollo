/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

/**
 * @author alukin@gmail.com
 */
public enum PeerTrustLevel {
    NOT_TRUSTED(0), REGISTERED(1), TRUSTED(2), SYSTEM_TRUSTED(3);
    private final int code;

    private PeerTrustLevel(int code) {
        this.code = code;
    }

    public long getCode() {
        return code;
    }

}
