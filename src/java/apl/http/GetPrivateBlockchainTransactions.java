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

import apl.*;
import apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetPrivateBlockchainTransactions extends APIServlet.APIRequestHandler {

    static final GetPrivateBlockchainTransactions instance = new GetPrivateBlockchainTransactions();

    private GetPrivateBlockchainTransactions() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS},  "height", "firstIndex", "lastIndex", "type", "subtype", "sharedKey", "account");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        byte[] sharedKey = ParameterParser.getBytes(req, "sharedKey", true);
        long accountId = ParameterParser.getAccountId(req, true);
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
            block.getTransactions().forEach(transaction-> {
                if (transaction.getType() == TransactionType.Payment.PRIVATE && transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId) {
                    transactions.add(JSONData.transaction(true, transaction));
                } else {
                    transactions.add(JSONData.encryptedTransaction(transaction, sharedKey));
                }
            });
        } else {
            try (DbIterator<? extends Transaction> iterator = Apl.getBlockchain().getTransactions(
                    accountId, 0, type, subtype, 0, false, false,
                    false, firstIndex, lastIndex, false, false, true)) {
                while (iterator.hasNext()) {
                    Transaction transaction = iterator.next();
                    if (TransactionType.Payment.PRIVATE == transaction.getType()) {
                        transactions.add(JSONData.encryptedTransaction(transaction, sharedKey));
                    } else {
                        transactions.add(JSONData.transaction(false, transaction));
                    }
                }
            }
        }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        return response;
    }

}
