/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.http;

import apl.AccountLedger;
import apl.AccountLedger.LedgerEntry;
import apl.AplException;
import apl.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static apl.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;

public class GetPrivateAccountLedgerEntry extends APIServlet.APIRequestHandler {

    /**
     * GetPrivateAccountLedgerEntry instance
     */
    private static class GetPrivateAccountLedgerEntryHolder {
        private static final GetPrivateAccountLedgerEntry INSTANCE = new GetPrivateAccountLedgerEntry();
    }

    public static GetPrivateAccountLedgerEntry getInstance() {
        return GetPrivateAccountLedgerEntryHolder.INSTANCE;
    }

    /**
     * Create the GetPrivateAccountLedgerEntry instance
     */
    private GetPrivateAccountLedgerEntry() {
        super(new APITag[]{APITag.ACCOUNTS}, "ledgerId", "includeTransaction", "includeHoldingInfo", "secretPhrase", "publicKey");
    }

    /**
     * Process the GetPrivateAccountLedgerEntry API request
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
        ParameterParser.PrivateTransactionsAPIData data = ParameterParser.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }
        long ledgerId = ParameterParser.getUnsignedLong(req, "ledgerId", true);
        boolean includeTransaction = "true".equalsIgnoreCase(req.getParameter("includeTransaction"));
        boolean includeHoldingInfo = "true".equalsIgnoreCase(req.getParameter("includeHoldingInfo"));

        //
        // Get the ledger entry
        //
        LedgerEntry ledgerEntry = AccountLedger.getEntry(ledgerId, true);
        if (ledgerEntry == null || ledgerEntry.getAccountId() != data.getAccountId())
            return JSONResponses.UNKNOWN_ENTRY;
        //
        // Return the response
        //
        JSONObject response = new JSONObject();
        JSONData.ledgerEntry(response, ledgerEntry, includeTransaction, includeHoldingInfo);
        if (data.isEncrypt()) {
            response = JSONData.encryptedLedgerEntry(response, data.getSharedKey());
            response.put("serverPublicKey", Convert.toHexString(API.getServerPublicKey()));
        }
        return response;
    }

    /**
     * No required block parameters
     *
     * @return FALSE to disable the required block parameters
     */
    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }
}
