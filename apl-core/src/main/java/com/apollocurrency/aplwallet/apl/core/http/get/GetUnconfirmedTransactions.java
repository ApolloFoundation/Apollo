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

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Payment;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.db.FilteringIterator;
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
import java.util.Set;

public final class GetUnconfirmedTransactions extends AbstractAPIRequestHandler {

    private static class GetUnconfirmedTransactionsHolder {
        private static final GetUnconfirmedTransactions INSTANCE = new GetUnconfirmedTransactions();
    }

    public static GetUnconfirmedTransactions getInstance() {
        return GetUnconfirmedTransactionsHolder.INSTANCE;
    }

    private GetUnconfirmedTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Set<Long> accountIds = Convert.toSet(ParameterParser.getAccountIds(req, false));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        if (accountIds.isEmpty()) {
            try (FilteringIterator<? extends Transaction> transactionsIterator = new FilteringIterator<> (
                    lookupTransactionProcessor().getAllUnconfirmedTransactions(0, -1),
                    transaction -> transaction.getType() != Payment.PRIVATE,
                    firstIndex, lastIndex)) {
                while (transactionsIterator.hasNext()) {
                    Transaction transaction = transactionsIterator.next();
                    transactions.add(JSONData.unconfirmedTransaction(transaction));
                }
            }
        } else {
            try (FilteringIterator<? extends Transaction> transactionsIterator = new FilteringIterator<> (
                    lookupTransactionProcessor().getAllUnconfirmedTransactions(0, -1),
                    transaction -> transaction.getType() != Payment.PRIVATE && (accountIds.contains(transaction.getSenderId()) ||
                            accountIds.contains(transaction.getRecipientId())),
                    firstIndex, lastIndex)) {
                while (transactionsIterator.hasNext()) {
                    Transaction transaction = transactionsIterator.next();
                    transactions.add(JSONData.unconfirmedTransaction(transaction));
                }
            }
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactions);
        return response;
    }

}
