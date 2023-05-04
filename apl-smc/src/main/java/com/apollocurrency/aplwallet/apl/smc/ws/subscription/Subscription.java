/*
 * Copyright (c) 2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws.subscription;

import com.apollocurrency.smc.data.expr.Term;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Getter
@Builder
@ToString
@EqualsAndHashCode
public class Subscription {
    private String requestId;
    private String subscriptionId;
    private String name;
    private String signature;
    private long fromBlock;
    private Term filter;
}
