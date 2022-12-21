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

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.MISSING_SIGNATURE_HASH;
import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionUtils.calculateUnsignedFullHash;

@Vetoed
public final class CalculateFullHash extends AbstractAPIRequestHandler {

    public CalculateFullHash() {
        super(new APITag[]{APITag.TRANSACTIONS}, "unsignedTransactionBytes", "unsignedTransactionJSON", "signatureHash");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String unsignedBytesString = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String signatureHashString = Convert.emptyToNull(req.getParameter("signatureHash"));
        String unsignedTransactionJSONString = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));

        if (signatureHashString == null) {
            return MISSING_SIGNATURE_HASH;
        }
        byte[] signatureHash = Convert.parseHexString(signatureHashString);
        JSONObject response = new JSONObject();
        try {
            Transaction transaction = HttpParameterParserUtil.parseTransaction(unsignedTransactionJSONString, unsignedBytesString, null);

            Result unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
            txBContext.createSerializer(transaction.getVersion())
                .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), unsignedTxBytes);

            byte[] fullHash = calculateUnsignedFullHash(unsignedTxBytes.array(), signatureHash);

            response.put("fullHash", Convert.toHexString(fullHash));
        } catch (RuntimeException e) {
            JSONData.putException(response, e, "Incorrect unsigned transaction json or bytes");
        }
        return response;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
