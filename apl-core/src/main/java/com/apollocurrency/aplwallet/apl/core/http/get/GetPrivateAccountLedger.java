/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.LedgerHolding;
import com.apollocurrency.aplwallet.apl.core.account.model.LedgerEntry;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;

@Vetoed
public class GetPrivateAccountLedger extends AbstractAPIRequestHandler {

    /**
     * Create the GetPrivateAccountLedger instance
     */
    public GetPrivateAccountLedger() {
        super(new APITag[]{APITag.ACCOUNTS}, "firstIndex", "lastIndex",
            "eventType", "event", "holdingType", "holding", "includeTransactions", "includeHoldingInfo", "secretPhrase", "publicKey");
    }

    /**
     * Process the GetPrivateAccountLedger API request
     *
     * @param req API request
     * @return API response
     * @throws AplException Invalid request
     */
    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        //
        // Process the request parameters
        //
        HttpParameterParserUtil.PrivateTransactionsAPIData data = HttpParameterParserUtil.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        String eventType = Convert.emptyToNull(req.getParameter("eventType"));
        LedgerEvent event = null;
        long eventId = 0;
        if (eventType != null) {
            try {
                event = LedgerEvent.valueOf(eventType);
                eventId = HttpParameterParserUtil.getUnsignedLong(req, "event", false);
            } catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.incorrect("eventType"));
            }
        }
        String holdingType = Convert.emptyToNull(req.getParameter("holdingType"));
        LedgerHolding holding = null;
        long holdingId = 0;
        if (holdingType != null) {
            try {
                holding = LedgerHolding.valueOf(holdingType);
                holdingId = HttpParameterParserUtil.getUnsignedLong(req, "holding", false);
            } catch (RuntimeException e) {
                throw new ParameterException(JSONResponses.incorrect("holdingType"));
            }
        }
        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));
        boolean includeHoldingInfo = "true".equalsIgnoreCase(req.getParameter("includeHoldingInfo"));

        //
        // Get the ledger entries
        //
        List<LedgerEntry> ledgerEntries = lookupAccountLedgerService().getEntries(data.getAccountId(), event, eventId,
            holding, holdingId, firstIndex, lastIndex, true);
        //
        // Return the response
        //
        JSONArray responseEntries = new JSONArray();
        Map<Long, List<LedgerEntry>> ledgerEntriesByEventId = ledgerEntries.stream().collect(Collectors.groupingBy(LedgerEntry::getEventId));
        ledgerEntries.forEach(entry -> {
            JSONObject responseEntry = new JSONObject();
            JSONData.ledgerEntry(responseEntry, entry, includeTransactions, includeHoldingInfo);
            if (data.isEncrypt() && (entry.getEvent() == LedgerEvent.PRIVATE_PAYMENT || ledgerEntriesByEventId.get(entry.getEventId()).stream().anyMatch(e -> e.getEvent() == LedgerEvent.PRIVATE_PAYMENT))) {
                responseEntries.add(JSONData.encryptedLedgerEntry(responseEntry, data.getSharedKey()));
            } else {
                responseEntries.add(responseEntry);
            }
        });

        JSONObject response = new JSONObject();
        response.put("entries", responseEntries);
        response.put("serverPublicKey", Convert.toHexString(elGamal.getServerPublicKey()));
        return response;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}
