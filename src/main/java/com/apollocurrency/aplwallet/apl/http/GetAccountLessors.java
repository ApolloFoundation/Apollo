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

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAccountLessors extends APIServlet.APIRequestHandler {

    private static class GetAccountLessorsHolder {
        private static final GetAccountLessors INSTANCE = new GetAccountLessors();
    }

    public static GetAccountLessors getInstance() {
        return GetAccountLessorsHolder.INSTANCE;
    }

    private GetAccountLessors() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "height");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account account = ParameterParser.getAccount(req);
        int height = ParameterParser.getHeight(req);
        if (height < 0) {
            height = Apl.getBlockchain().getHeight();
        }

        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", account.getId());
        response.put("height", height);
        JSONArray lessorsJSON = new JSONArray();

        try (DbIterator<Account> lessors = account.getLessors(height)) {
            if (lessors.hasNext()) {
                while (lessors.hasNext()) {
                    Account lessor = lessors.next();
                    JSONObject lessorJSON = new JSONObject();
                    JSONData.putAccount(lessorJSON, "lessor", lessor.getId());
                    lessorJSON.put("guaranteedBalanceATM", String.valueOf(lessor.getGuaranteedBalanceATM(Constants.getGuaranteedBalanceConfirmations(), height)));
                    lessorsJSON.add(lessorJSON);
                }
            }
        }
        response.put("lessors", lessorsJSON);
        return response;

    }

}
