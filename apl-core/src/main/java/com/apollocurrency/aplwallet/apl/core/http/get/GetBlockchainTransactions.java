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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.util.api.Sort;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.PRIVATE_TRANSACTIONS_ACCESS_DENIED;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT;

@Vetoed
public final class GetBlockchainTransactions extends AbstractAPIRequestHandler {

    public GetBlockchainTransactions() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.TRANSACTIONS}, "account", "timestamp", "type", "subtype",
            "firstIndex", "lastIndex", "numberOfConfirmations", "withMessage", "phasedOnly", "nonPhasedOnly",
            "includeExpiredPrunable", "includePhasingResult", "executedOnly", "failedOnly", "nonFailedOnly", "sort");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long accountId = HttpParameterParserUtil.getAccountId(req, true);
        int timestamp = HttpParameterParserUtil.getTimestamp(req);
        int numberOfConfirmations = HttpParameterParserUtil.getNumberOfConfirmations(req);
        boolean withMessage = "true".equalsIgnoreCase(req.getParameter("withMessage"));
        boolean phasedOnly = "true".equalsIgnoreCase(req.getParameter("phasedOnly"));
        boolean nonPhasedOnly = "true".equalsIgnoreCase(req.getParameter("nonPhasedOnly"));
        boolean includeExpiredPrunable = "true".equalsIgnoreCase(req.getParameter("includeExpiredPrunable"));
        boolean includePhasingResult = "true".equalsIgnoreCase(req.getParameter("includePhasingResult"));
        boolean executedOnly = "true".equalsIgnoreCase(req.getParameter("executedOnly"));
        boolean failedOnly = "true".equalsIgnoreCase(req.getParameter("failedOnly"));
        boolean nonFailedOnly = "true".equalsIgnoreCase(req.getParameter("nonFailedOnly"));
        String sort = HttpParameterParserUtil.getStringParameter(req, "sort", false);

        if (sort == null) {
            sort = "DESC";
        }
        Sort sortObj = new Sort(sort);

        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter("type"));
        } catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter("subtype"));
        } catch (NumberFormatException e) {
            subtype = -1;
        }
        if (PRIVATE_PAYMENT.getType() == type && PRIVATE_PAYMENT.getSubtype() == subtype) {
            return PRIVATE_TRANSACTIONS_ACCESS_DENIED;
        }
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        List<Transaction> transactionList = lookupBlockchain().getTransactions(accountId, numberOfConfirmations,
            type, subtype, timestamp, withMessage, phasedOnly, nonPhasedOnly, firstIndex, lastIndex,
            includeExpiredPrunable, executedOnly, false, failedOnly, nonFailedOnly, sortObj);
        transactionList.forEach(tx -> transactions.add(JSONData.transaction(tx, includePhasingResult, false)));

        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;

    }

}
