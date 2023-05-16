/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.post.CreateTransactionHandler;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;

@Singleton
public class DexOrderTransactionCreator extends CreateTransactionHandler {

    private DexOrderTransactionCreator() {
        super(new APITag[]{APITag.CREATE_TRANSACTION}, "orderType", "offerAmount", "offerCurrency", "pairCurrency", "pairRate", "finishTime");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        throw new UnsupportedOperationException();
    }
}
