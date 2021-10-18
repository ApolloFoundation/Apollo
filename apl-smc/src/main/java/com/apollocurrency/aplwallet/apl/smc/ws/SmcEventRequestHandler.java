/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventResponse;
import com.apollocurrency.aplwallet.apl.smc.ws.dto.SmcEventSubscriptionRequest;

/**
 * @author andrew.zinchenko@gmail.com
 */
public abstract class SmcEventRequestHandler {

    public abstract SmcEventResponse process(SmcEventSubscriptionRequest request);

}
