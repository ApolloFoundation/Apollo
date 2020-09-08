/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import lombok.Data;

@Data
public class UnconfirmedTxValidationResult {
    private final long code;
    private final Error error;
    private final String errorDescription;

    public boolean isOk() {
        return code == 0;
    }

    public enum Error {
        ALREADY_PROCESSED,
        NOT_VALID,
        NOT_CURRENTLY_VALID,

    }
}
