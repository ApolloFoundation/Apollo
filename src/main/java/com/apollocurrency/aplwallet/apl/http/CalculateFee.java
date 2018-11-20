/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONStreamAware;

public class CalculateFee extends APIServlet.APIRequestHandler {
    private static class CalculateFeeHolder {
        private static final CalculateFee INSTANCE = new CalculateFee();
    }

    public static CalculateFee getInstance() {
        return CalculateFeeHolder.INSTANCE;
    }
    private CalculateFee() {
        super(new APITag[] {APITag.TRANSACTIONS}, "type", "subtype", "message");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {

    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }
}
