/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface SmcEventSocketListener {
    void onClose(SmcEventSocket socket, int code, String message);

    void onOpen(SmcEventSocket socket);

    void onMessage(SmcEventSocket socket, SmcEventSubscriptionRequest message);
}
