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
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Attachment;
import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.Transaction;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static com.apollocurrency.aplwallet.apl.http.JSONResponses.HASHES_MISMATCH;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.INCORRECT_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class VerifyTaggedData extends APIServlet.APIRequestHandler {

    private static class VerifyTaggedDataHolder {
        private static final VerifyTaggedData INSTANCE = new VerifyTaggedData();
    }

    public static VerifyTaggedData getInstance() {
        return VerifyTaggedDataHolder.INSTANCE;
    }

    private VerifyTaggedData() {
        super("file", new APITag[]{APITag.DATA}, "transaction",
                "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        Transaction transaction = Apl.getBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }

        Attachment.TaggedDataUpload taggedData = ParameterParser.getTaggedData(req);
        Attachment attachment = transaction.getAttachment();

        if (!(attachment instanceof Attachment.TaggedDataUpload)) {
            return INCORRECT_TRANSACTION;
        }

        Attachment.TaggedDataUpload myTaggedData = (Attachment.TaggedDataUpload) attachment;
        if (!Arrays.equals(myTaggedData.getHash(), taggedData.getHash())) {
            return HASHES_MISMATCH;
        }

        JSONObject response = myTaggedData.getJSONObject();
        response.put("verify", true);
        return response;
    }

}
