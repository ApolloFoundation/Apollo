/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.Transaction;
import com.apollocurrency.aplwallet.apl.TransactionType;
import com.apollocurrency.aplwallet.apl.db.FilteringIterator;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetPrivateUnconfirmedTransactions extends APIServlet.APIRequestHandler {

    private static class GetPrivateUnconfirmedTransactionsHolder {
        private static final GetPrivateUnconfirmedTransactions INSTANCE = new GetPrivateUnconfirmedTransactions();
    }

    public static GetPrivateUnconfirmedTransactions getInstance() {
        return GetPrivateUnconfirmedTransactionsHolder.INSTANCE;
    }

    private GetPrivateUnconfirmedTransactions() {
        super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, "firstIndex", "lastIndex", "secretPhrase", "publicKey");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        ParameterParser.PrivateTransactionsAPIData data = ParameterParser.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        JSONArray transactions = new JSONArray();
        try (FilteringIterator<? extends Transaction> transactionsIterator = new FilteringIterator<>(
                Apl.getTransactionProcessor().getAllUnconfirmedTransactions(0, -1),
                transaction -> data.getAccountId() == transaction.getSenderId() || data.getAccountId() == transaction.getRecipientId(),
                firstIndex, lastIndex)) {
            while (transactionsIterator.hasNext()) {
                Transaction transaction = transactionsIterator.next();
                if (data.isEncrypt() && transaction.getType() == TransactionType.Payment.PRIVATE) {
                    transactions.add(JSONData.encryptedUnconfirmedTransaction(transaction, data.getSharedKey()));
                } else {
                    transactions.add(JSONData.unconfirmedTransaction(transaction));
                }
            }
        }
        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactions);
        response.put("serverPublicKey", Convert.toHexString(API.getServerPublicKey()));
        return response;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}

