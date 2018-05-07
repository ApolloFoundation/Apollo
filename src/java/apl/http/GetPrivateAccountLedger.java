/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation B.V.,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.http;

import apl.Account;
import apl.AccountLedger;
import apl.AccountLedger.LedgerEntry;
import apl.AccountLedger.LedgerEvent;
import apl.AccountLedger.LedgerHolding;
import apl.AplException;
import apl.crypto.Crypto;
import apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class GetPrivateAccountLedger extends APIServlet.APIRequestHandler {

    /**
     * GetPrivateAccountLedger instance
     */
    static final GetPrivateAccountLedger instance = new GetPrivateAccountLedger();

    /**
     * Create the GetPrivateAccountLedger instance
     */
    private GetPrivateAccountLedger() {
        super(new APITag[] {APITag.ACCOUNTS},  "firstIndex", "lastIndex",
                "eventType", "event", "holdingType", "holding", "includeTransactions", "includeHoldingInfo", "secretPhrase");
    }

    /**
     * Process the GetPrivateAccountLedger API request
     *
     * @param req API request
     * @return API response
     * @throws AplException Invalid request
     */
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        //
        // Process the request parameters
        //
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        String eventType = Convert.emptyToNull(req.getParameter("eventType"));
        LedgerEvent event = null;
        long eventId = 0;
        if (eventType != null) {
            try {
                event = LedgerEvent.valueOf(eventType);
                eventId = ParameterParser.getUnsignedLong(req, "event", false);
            }
            catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.incorrect("eventType"));
            }
        }
        String holdingType = Convert.emptyToNull(req.getParameter("holdingType"));
        LedgerHolding holding = null;
        long holdingId = 0;
        if (holdingType != null) {
            try {
                holding = LedgerHolding.valueOf(holdingType);
                holdingId = ParameterParser.getUnsignedLong(req, "holding", false);
            }
            catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.incorrect("holdingType"));
            }
        }
        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));
        boolean includeHoldingInfo = "true".equalsIgnoreCase(req.getParameter("includeHoldingInfo"));

        //
        // Get the ledger entries
        //
        List<LedgerEntry> ledgerEntries = AccountLedger.getEntries(accountId, event, eventId,
                holding, holdingId, firstIndex, lastIndex);
        //
        // Return the response
        //
        JSONArray responseEntries = new JSONArray();
        ledgerEntries.forEach(entry -> {
            JSONObject responseEntry = new JSONObject();
            JSONData.ledgerEntry(responseEntry, entry, includeTransactions, includeHoldingInfo);
            responseEntries.add(responseEntry);
        });

        JSONObject response = new JSONObject();
        response.put("entries", responseEntries);
        return response;
    }
}
