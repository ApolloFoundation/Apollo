/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetGuaranteedBalance extends APIServlet.APIRequestHandler {

    private static class GetGuaranteedBalanceHolder {
        private static final GetGuaranteedBalance INSTANCE = new GetGuaranteedBalance();
    }

    public static GetGuaranteedBalance getInstance() {
        return GetGuaranteedBalanceHolder.INSTANCE;
    }

    private GetGuaranteedBalance() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.FORGING}, "account", "numberOfConfirmations");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account account = ParameterParser.getAccount(req);
        int numberOfConfirmations = ParameterParser.getNumberOfConfirmations(req);

        JSONObject response = new JSONObject();
        if (account == null) {
            response.put("guaranteedBalanceATM", "0");
        } else {
            response.put("guaranteedBalanceATM", String.valueOf(account.getGuaranteedBalanceATM(numberOfConfirmations, Apl.getBlockchain().getHeight())));
        }

        return response;
    }

}
