/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventReceipt;
import com.apollocurrency.aplwallet.apl.util.exception.Messages;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class SmcErrorReceipt {
    private SmcErrorReceipt() {
    }

    public static SmcEventReceipt error(ErrorInfo errorInfo, Object... args) {
        return error(null, errorInfo, args);
    }

    public static SmcEventReceipt error(String requestId, ErrorInfo errorInfo, Object... args) {
        String reasonPhrase = Messages.format(errorInfo.getErrorDescription(), args);
        return SmcEventReceipt.builder()
            .errorCode(errorInfo.getErrorCode())
            .errorDescription(reasonPhrase)
            .status(SmcEventReceipt.Status.ERROR)
            .requestId(requestId)
            .build();
    }
}
