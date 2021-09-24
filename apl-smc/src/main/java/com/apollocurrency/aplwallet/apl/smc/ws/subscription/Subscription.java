/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.aplwallet.apl.smc.ws.SmcEventSocket;
import com.apollocurrency.aplwallet.apl.smc.ws.expr.Term;
import lombok.Builder;
import lombok.Getter;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Builder
public class Subscription {
    private SmcEventSocket socket;
    private String requestId;
    private String subscriptionId;
    private String name;
    private String signature;
    private long fromBlock;
    private Term filter;
}
