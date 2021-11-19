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

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSigner;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSignerImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class SignTransaction extends AbstractAPIRequestHandler {

    private static final TransactionValidator validator = CDI.current().select(TransactionValidator.class).get();
    private static final TransactionSigner signerService = CDI.current().select(TransactionSignerImpl.class).get();

    public SignTransaction() {
        super(new APITag[]{APITag.TRANSACTIONS}, "unsignedTransactionJSON", "unsignedTransactionBytes", "prunableAttachmentJSON", "secretPhrase",
            "validate", "sender", "passphrase");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionJSON = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));
        String transactionBytes = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String prunableAttachmentJSON = Convert.emptyToNull(req.getParameter("prunableAttachmentJSON"));
        long senderId = HttpParameterParserUtil.getAccountId(req, "sender", false);
        byte[] keySeed = HttpParameterParserUtil.getKeySeed(req, senderId, true);
        boolean validate = !"false".equalsIgnoreCase(req.getParameter("validate"));

        JSONObject response = new JSONObject();
        try {
            Transaction transaction = HttpParameterParserUtil.parseTransaction(transactionJSON, transactionBytes, prunableAttachmentJSON);

            signerService.sign(transaction, keySeed);

            Result signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(transaction.getVersion()).serialize(transaction, signedTxBytes);

            JSONObject signedTransactionJSON = JSONData.unconfirmedTransaction(transaction);
            if (validate) {
                validator.validateFully(transaction);
                response.put("verify", validator.verifySignature(transaction));
            }
            response.put("transactionJSON", signedTransactionJSON);
            response.put("fullHash", signedTransactionJSON.get("fullHash"));
            response.put("signatureHash", signedTransactionJSON.get("signatureHash"));
            response.put("transaction", transaction.getStringId());
            response.put("transactionBytes", Convert.toHexString(signedTxBytes.array()));
            JSONData.putPrunableAttachment(response, transaction);
        } catch (AplException.ValidationException | RuntimeException e) {
            JSONData.putException(response, e, "Incorrect unsigned transaction json or bytes");
        }
        return response;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
