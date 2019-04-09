/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.service;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.post.CreateTransaction;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;

@Singleton
public class DexOfferTransactionCreator extends CreateTransaction {

    private DexOfferTransactionCreator() {
        super(new APITag[] {APITag.CREATE_TRANSACTION}, "orderID");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        return null;
    }
}
