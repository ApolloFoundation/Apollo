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

import com.apollocurrency.aplwallet.apl.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;


public final class StopForging extends APIServlet.APIRequestHandler {

    private static class StopForgingHolder {
        private static final StopForging INSTANCE = new StopForging();
    }

    public static StopForging getInstance() {
        return StopForgingHolder.INSTANCE;
    }

    private StopForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase", "adminPassword");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long accountId = ParameterParser.getAccountId(req, vaultAccountName(), false);
        byte[] keySeed = ParameterParser.getKeySeed(req, accountId, false);
        JSONObject response = new JSONObject();
        if (keySeed != null) {
            Generator generator = Generator.stopForging(keySeed);
            response.put("foundAndStopped", generator != null);
            response.put("forgersCount", Generator.getGeneratorCount());
        } else {
            API.verifyPassword(req);
            int count = Generator.stopForging();
            response.put("stopped", count);
        }
        return response;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }


    @Override
    protected boolean is2FAProtected() {
        return true;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}
