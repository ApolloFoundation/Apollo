/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.post.CreateTransaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class DexOrderTransactionCreator extends CreateTransaction {

    private DexOrderTransactionCreator() {
        super(new APITag[]{APITag.CREATE_TRANSACTION}, "orderType", "offerAmount", "offerCurrency", "pairCurrency", "pairRate", "finishTime");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        throw new UnsupportedOperationException();
    }
}
