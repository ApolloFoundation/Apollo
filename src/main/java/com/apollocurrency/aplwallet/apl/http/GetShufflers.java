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
import com.apollocurrency.aplwallet.apl.Shuffler;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;


public final class GetShufflers extends APIServlet.APIRequestHandler {

    private static class GetShufflersHolder {
        private static final GetShufflers INSTANCE = new GetShufflers();
    }

    public static GetShufflers getInstance() {
        return GetShufflersHolder.INSTANCE;
    }

    private GetShufflers() {
        super(new APITag[] {APITag.SHUFFLING}, "account", "shufflingFullHash", "secretPhrase", "adminPassword", "includeParticipantState");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        byte[] shufflingFullHash = ParameterParser.getBytes(req, "shufflingFullHash", false);
        long accountId = ParameterParser.getAccountId(req, false);
        boolean includeParticipantState = "true".equalsIgnoreCase(req.getParameter("includeParticipantState"));
        List<Shuffler> shufflers;
        if (secretPhrase != null) {
            if (accountId != 0 && Account.getId(Crypto.getPublicKey(secretPhrase)) != accountId) {
                return JSONResponses.INCORRECT_ACCOUNT;
            }
            accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
            if (shufflingFullHash.length == 0) {
                shufflers = Shuffler.getAccountShufflers(accountId);
            } else {
                Shuffler shuffler = Shuffler.getShuffler(accountId, shufflingFullHash);
                shufflers = shuffler == null ? Collections.emptyList() : Collections.singletonList(shuffler);
            }
        } else {
            API.verifyPassword(req);
            if (accountId != 0 && shufflingFullHash.length == 0) {
                shufflers = Shuffler.getAccountShufflers(accountId);
            } else if (accountId == 0 && shufflingFullHash.length > 0) {
                shufflers = Shuffler.getShufflingShufflers(shufflingFullHash);
            } else if (accountId != 0 && shufflingFullHash.length > 0) {
                Shuffler shuffler = Shuffler.getShuffler(accountId, shufflingFullHash);
                shufflers = shuffler == null ? Collections.emptyList() : Collections.singletonList(shuffler);
            } else {
                shufflers = Shuffler.getAllShufflers();
            }
        }
        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        shufflers.forEach(shuffler -> jsonArray.add(JSONData.shuffler(shuffler, includeParticipantState)));
        response.put("shufflers", jsonArray);
        return response;
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
