/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.transaction.Payment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;

@Vetoed
public final class GetPrivateBlockchainTransactions extends AbstractAPIRequestHandler {

    public GetPrivateBlockchainTransactions() {
        super(new APITag[]{APITag.ACCOUNTS, APITag.TRANSACTIONS}, "height", "firstIndex", "lastIndex", "type", "subtype", "publicKey",
            "secretPhrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        HttpParameterParserUtil.PrivateTransactionsAPIData data = HttpParameterParserUtil.parsePrivateTransactionRequest(req);
        if (data == null) {
            return MISSING_SECRET_PHRASE_AND_PUBLIC_KEY;
        }

        int height = HttpParameterParserUtil.getHeight(req);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
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
        JSONArray transactions = new JSONArray();
        Blockchain blockchain = lookupBlockchain();
        if (height != -1) {
            Block block = blockchain.getBlockAtHeight(height);
            block.getOrLoadTransactions().forEach(transaction -> {
                if (transaction.getType() == Payment.PRIVATE) {

                    if (transaction.getSenderId() != data.getAccountId() && transaction.getRecipientId() != data.getAccountId()) {
                        transactions.add(JSONData.transaction(true, transaction));
                    } else if (data.isEncrypt()) {
                        transactions.add(JSONData.encryptedTransaction(transaction, data.getSharedKey()));

                    } else {
                        transactions.add(JSONData.transaction(false, transaction));
                    }
                } else {
                    transactions.add(JSONData.transaction(false, transaction));
                }
            });
        } else {
            List<Transaction> transactionList = blockchain.getTransactions(
                data.getAccountId(), 0, type, subtype, 0, false, false,
                false, firstIndex, lastIndex, false, false, true);
            transactionList.forEach(tx -> {

                if (Payment.PRIVATE == tx.getType() && data.isEncrypt()) {
                    transactions.add(JSONData.encryptedTransaction(tx, data.getSharedKey()));

                } else {
                    transactions.add(JSONData.transaction(false, tx));
                }
            });
        }
        JSONObject response = new JSONObject();
        response.put("transactions", transactions);
        response.put("serverPublicKey", Convert.toHexString(elGamal.getServerPublicKey()));
        return response;
    }

    @Override
    protected String vaultAccountName() {
        return "account";
    }
}
