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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.NOT_FORGING;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_ACCOUNT;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Account;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.Generator;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;


public final class GetForging extends APIServlet.APIRequestHandler {

    private static class GetForgingHolder {
        private static final GetForging INSTANCE = new GetForging();
    }

    public static GetForging getInstance() {
        return GetForgingHolder.INSTANCE;
    }

    private GetForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase", "adminPassword", "publicKey", "passphrase", "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long id = ParameterParser.getAccountId(req, "account", false);
        byte[] publicKey = ParameterParser.getPublicKey(req, null, id, false);
        int elapsedTime = Apl.getEpochTime() - Apl.getBlockchain().getLastBlock().getTimestamp();
        if (publicKey != null) {
            Account account = Account.getAccount(publicKey);
            if (account == null) {
                return UNKNOWN_ACCOUNT;
            }
            Generator generator = Generator.getGenerator(Convert.getId(publicKey));
            if (generator == null) {
                return NOT_FORGING;
            }
            return JSONData.generator(generator, elapsedTime);
        } else {
            API.verifyPassword(req);
            JSONObject response = new JSONObject();
            JSONArray generators = new JSONArray();
            Generator.getSortedForgers().forEach(generator -> generators.add(JSONData.generator(generator, elapsedTime)));
            response.put("generators", generators);
            return response;
        }
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

}
