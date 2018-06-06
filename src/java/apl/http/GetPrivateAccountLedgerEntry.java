/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
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
import apl.AplException;
import apl.crypto.Crypto;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetPrivateAccountLedgerEntry extends APIServlet.APIRequestHandler {

    /** GetPrivateAccountLedgerEntry instance */
    static final GetPrivateAccountLedgerEntry instance = new GetPrivateAccountLedgerEntry();

    /**
     * Create the GetPrivateAccountLedgerEntry instance
     */
    private GetPrivateAccountLedgerEntry() {
        super(new APITag[] {APITag.ACCOUNTS}, "ledgerId", "includeTransaction", "includeHoldingInfo", "secretPhrase");
    }

    /**
     * Process the GetPrivateAccountLedgerEntry API request
     *
     * @param   req                 API request
     * @return                      API response
     * @throws  AplException        Invalid request
     */
    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        //
        // Process the request parameters
        //
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        Long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        long ledgerId = ParameterParser.getUnsignedLong(req, "ledgerId", true);
        boolean includeTransaction = "true".equalsIgnoreCase(req.getParameter("includeTransaction"));
        boolean includeHoldingInfo = "true".equalsIgnoreCase(req.getParameter("includeHoldingInfo"));

        //
        // Get the ledger entry
        //
        LedgerEntry ledgerEntry = AccountLedger.getEntry(ledgerId, true);
        if (ledgerEntry == null || ledgerEntry.getAccountId() != accountId)
            return JSONResponses.UNKNOWN_ENTRY;
        //
        // Return the response
        //
        JSONObject response = new JSONObject();
        JSONData.ledgerEntry(response, ledgerEntry, includeTransaction, includeHoldingInfo);
        return response;
    }

    /**
     * No required block parameters
     *
     * @return                      FALSE to disable the required block parameters
     */
    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
