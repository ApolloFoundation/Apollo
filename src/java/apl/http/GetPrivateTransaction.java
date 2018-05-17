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
import apl.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static apl.http.JSONResponses.*;

public final class GetPrivateTransaction extends APIServlet.APIRequestHandler {

    static final GetPrivateTransaction instance = new GetPrivateTransaction();

    private GetPrivateTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transaction", "fullHash", "secretPhrase");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        String secretPhrase = ParameterParser.getSecretPhrase(req, true);
        long accountId = Account.getId(Crypto.getPublicKey(secretPhrase));
        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        String transactionFullHash = Convert.emptyToNull(req.getParameter("fullHash"));
        if (transactionIdString == null && transactionFullHash == null) {
            return MISSING_TRANSACTION;
        }
        long transactionId = 0;
        Transaction transaction;
        try {
            if (transactionIdString != null) {
                transactionId = Convert.parseUnsignedLong(transactionIdString);
                transaction = Apl.getBlockchain().getTransaction(transactionId);
            } else {
                transaction = Apl.getBlockchain().getTransactionByFullHash(transactionFullHash);
                if (transaction == null) {
                    return UNKNOWN_TRANSACTION;
                }
            }
        }
        catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }
        if (transaction == null) {
            transaction = Apl.getTransactionProcessor().getUnconfirmedTransaction(transactionId);
            if (transaction == null || !transaction.getType().equals(TransactionType.Payment.PRIVATE) || transaction.getType().equals(TransactionType.Payment.PRIVATE) && (transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId )) {
                return UNKNOWN_TRANSACTION;
            }
            return JSONData.unconfirmedTransaction(transaction);
        } else {
            if (!transaction.getType().equals(TransactionType.Payment.PRIVATE) || transaction.getType().equals(TransactionType.Payment.PRIVATE) && (transaction.getSenderId() != accountId && transaction.getRecipientId() != accountId )) {
                return UNKNOWN_TRANSACTION;
            }
            return JSONData.transaction(transaction, false, false);
        }

    }

}

