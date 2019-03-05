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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.NOT_FORGING;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_ACCOUNT;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.app.Generator;
import com.apollocurrency.aplwallet.apl.core.http.API;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public final class GetForging extends AbstractAPIRequestHandler {

    private static class GetForgingHolder {
        private static final GetForging INSTANCE = new GetForging();
    }

    public static GetForging getInstance() {
        return GetForgingHolder.INSTANCE;
    }

    private GetForging() {
        super(new APITag[] {APITag.FORGING}, "secretPhrase", "adminPassword", "publicKey");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        long id = ParameterParser.getAccountId(req, vaultAccountName(), false);
        byte[] publicKey = ParameterParser.getPublicKey(req, null, id, false);
        int elapsedTime = timeService.getEpochTime() - lookupBlockchain().getLastBlock().getTimestamp();
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

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}
