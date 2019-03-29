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

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

@Vetoed
public final class SignTransaction extends AbstractAPIRequestHandler {

    public SignTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "unsignedTransactionJSON", "unsignedTransactionBytes", "prunableAttachmentJSON", "secretPhrase",
                "validate", "sender", "passphrase");
    }
    private static TransactionValidator validator = CDI.current().select(TransactionValidator.class).get();

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionJSON = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));
        long senderId = ParameterParser.getAccountId(req, "sender", false);
        Transaction.Builder builder = ParameterParser.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON);

        byte[] keySeed = ParameterParser.getKeySeed(req, senderId, true);
        boolean validate = !"false".equalsIgnoreCase(req.getParameter("validate"));

        JSONObject response = new JSONObject();
        try {
            Transaction transaction = builder.build(keySeed);
            JSONObject signedTransactionJSON = JSONData.unconfirmedTransaction(transaction);
            if (validate) {
                validator.validate(transaction);
                response.put("verify", transaction.verifySignature());
            }
            response.put("transactionJSON", signedTransactionJSON);
            response.put("fullHash", signedTransactionJSON.get("fullHash"));
            response.put("signatureHash", signedTransactionJSON.get("signatureHash"));
            response.put("transaction", transaction.getStringId());
            response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            JSONData.putPrunableAttachment(response, transaction);
        } catch (AplException.ValidationException|RuntimeException e) {
            JSONData.putException(response, e, "Incorrect unsigned transaction json or bytes");
        }
        return response;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
