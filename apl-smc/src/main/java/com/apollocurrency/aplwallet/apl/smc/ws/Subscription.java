/*
 * Copyright (c) 2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.smc.ws;

/**
 * @author andrew.zinchenko@gmail.com
 */
public class Subscription {
    private SmcEventSocket socket;
    private String requestId;
    private long fromBlock;
    private SmcEventSubscriptionRequest request;
}
