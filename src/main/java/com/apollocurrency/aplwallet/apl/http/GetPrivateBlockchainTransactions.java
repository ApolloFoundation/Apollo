/*
 * Copyright © 2017-2018 Apollo Foundation
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

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.*;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;

public final class GetPrivateBlockchainTransactions extends APIServlet.APIRequestHandler {

    private static class GetPrivateBlockchainTransactionsHolder {
        private static final GetPrivateBlockchainTransactions INSTANCE = new GetPrivateBlockchainTransactions();
    }

    public static GetPrivateBlockchainTransactions getInstance() {
        return GetPrivateBlockchainTransactionsHolder.INSTANCE;
    }

    private GetPrivateBlockchainTransactions() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS},  "height", "firstIndex", "lastIndex", "type", "subtype", "publicKey", "secretPhrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        ParameterParser.PrivateTransactionsAPIData data = ParameterParser.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }

        int height = ParameterParser.getHeight(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        byte type;
        byte subtype;
        try {
            type = Byte.parseByte(req.getParameter("type"));
        }
        catch (NumberFormatException e) {
            type = -1;
        }
        try {
            subtype = Byte.parseByte(req.getParameter("subtype"));
        }
        catch (NumberFormatException e) {
            subtype = -1;
        }
        JSONArray transactions = new JSONArray();
        if (height != -1) {
            Block block = Apl.getBlockchain().getBlockAtHeight(height);
            block.getTransactions().forEach(transaction -> {
                if (transaction.getType() == TransactionType.Payment.PRIVATE) {

                    if (transaction.getSenderId() != data.getAccountId() && transaction.getRecipientId() != data.getAccountId()) {
                        transactions.add(JSONData.transaction(true, transaction));
                    } else if (data.isEncrypt()){
                        transactions.add(JSONData.encryptedTransaction(transaction, data.getSharedKey()));

                    } else {
                        transactions.add(JSONData.transaction(false, transaction));
                    }
                } else {
                    transactions.add(JSONData.transaction(false, transaction));
                }
            });
        } else {
            try (DbIterator<? extends Transaction> iterator = Apl.getBlockchain().getTransactions(
                    data.getAccountId(), 0, type, subtype, 0, false, false,
                    false, firstIndex, lastIndex, false, false, true)) {
                while (iterator.hasNext()) {
                    Transaction transaction = iterator.next();

                    if (TransactionType.Payment.PRIVATE == transaction.getType() && data.isEncrypt()) {
                        transactions.add(JSONData.encryptedTransaction(transaction, data.getSharedKey()));

                    } else {
                        transactions.add(JSONData.transaction(false, transaction));
                    }
                }
            }
        }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        response.put("serverPublicKey", Convert.toHexString(API.getServerPublicKey()));
        return response;
    }

}
