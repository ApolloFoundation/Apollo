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
import apl.crypto.Crypto;
import apl.db.DbIterator;
import apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static apl.http.JSONResponses.incorrect;
import static apl.http.JSONResponses.missing;

public final class GetPrivateBlockchainTransactions extends APIServlet.APIRequestHandler {

    static final GetPrivateBlockchainTransactions instance = new GetPrivateBlockchainTransactions();

    private GetPrivateBlockchainTransactions() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.TRANSACTIONS},  "height", "firstIndex", "lastIndex", "type", "subtype", "account", "signature", "secretPhrase", "message");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        long account = ParameterParser.getAccountId(req, false);
        byte[] signature = Convert.emptyToNull(ParameterParser.getBytes(req, "signature", false));
        byte[] message = Convert.emptyToNull(ParameterParser.getBytes(req, "message", false));
        String secretPhrase = ParameterParser.getSecretPhrase(req, false);
        byte[] publicKey;
        boolean encrypt;
        //prefer request without secretPhrase
        if (account != 0 && signature != null && message != null) {
            publicKey = Account.getPublicKey(account);
            if (publicKey == null) {
                return incorrect("Public key", "Your account has no public key");
            }
            if (!Crypto.verify(signature, message, publicKey)) {
                return incorrect("Signature");
            }
            encrypt = true;
        } else if (secretPhrase != null) {
            publicKey = Crypto.getPublicKey(secretPhrase);
            account = Account.getId(publicKey);
            if (Account.getPublicKey(account) == null) {
                return incorrect("Public key", "Your account has no public key");
            }
            encrypt = false;
        } else {
            return missing("Secret phrase", "Account + signature + message");
        }
        long accountId = account;
        byte[] sharedKey = Crypto.getSharedKey(API.getServerPrivateKey(), publicKey);
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
                    if (transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId) {
                        transactions.add(JSONData.transaction(true, transaction));
                    } else if (encrypt){
                        transactions.add(JSONData.encryptedTransaction(transaction, sharedKey));
                    } else {
                        transactions.add(JSONData.transaction(false, transaction));
                    }
                } else {
                    transactions.add(JSONData.transaction(false, transaction));
                }
            });
        } else {
            try (DbIterator<? extends Transaction> iterator = Apl.getBlockchain().getTransactions(
                    accountId, 0, type, subtype, 0, false, false,
                    false, firstIndex, lastIndex, false, false, true)) {
                while (iterator.hasNext()) {
                    Transaction transaction = iterator.next();
                    if (TransactionType.Payment.PRIVATE == transaction.getType() && encrypt) {
                        transactions.add(JSONData.encryptedTransaction(transaction, sharedKey));
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
