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

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_DATE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_HOST;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_WEIGHT;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_DATE;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_HOST;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_WEIGHT;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Constants;
import com.apollocurrency.aplwallet.apl.peer.Hallmark;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;


public final class MarkHost extends APIServlet.APIRequestHandler {

    private static class MarkHostHolder {
        private static final MarkHost INSTANCE = new MarkHost();
    }

    public static MarkHost getInstance() {
        return MarkHostHolder.INSTANCE;
    }

    private MarkHost() {
        super(new APITag[] {APITag.TOKENS}, "secretPhrase", "host", "weight", "date", "account", "passphrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long accountId = ParameterParser.getAccountId(req, false);
        byte[] keySeed = ParameterParser.getKeySeed(req, accountId, true);
        String host = Convert.emptyToNull(req.getParameter("host"));
        String weightValue = Convert.emptyToNull(req.getParameter("weight"));
        String dateValue = Convert.emptyToNull(req.getParameter("date"));
        if (host == null) {
            return MISSING_HOST;
        } else if (weightValue == null) {
            return MISSING_WEIGHT;
        } else if (dateValue == null) {
            return MISSING_DATE;
        }

        if (host.length() > 100) {
            return INCORRECT_HOST;
        }

        int weight;
        try {
            weight = Integer.parseInt(weightValue);
            if (weight <= 0 || weight > Constants.MAX_BALANCE_APL) {
                return INCORRECT_WEIGHT;
            }
        } catch (NumberFormatException e) {
            return INCORRECT_WEIGHT;
        }

        try {

            String hallmark = Hallmark.generateHallmark(keySeed, host, weight, Hallmark.parseDate(dateValue));

            JSONObject response = new JSONObject();
            response.put("hallmark", hallmark);
            return response;

        } catch (RuntimeException e) {
            return INCORRECT_DATE;
        }

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
    protected boolean requireBlockchain() {
        return false;
    }

}
