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

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.HASHES_MISMATCH;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.INCORRECT_TRANSACTION;
import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_TRANSACTION;

@Vetoed
public final class VerifyTaggedData extends AbstractAPIRequestHandler {

    public VerifyTaggedData() {
        super("file", new APITag[]{APITag.DATA}, "transaction",
            "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        long transactionId = HttpParameterParserUtil.getUnsignedLong(req, "transaction", true);
        Transaction transaction = lookupBlockchain().getTransaction(transactionId);
        if (transaction == null) {
            return UNKNOWN_TRANSACTION;
        }

        TaggedDataUploadAttachment taggedData = HttpParameterParserUtil.getTaggedData(req);
        Attachment attachment = transaction.getAttachment();

        if (!(attachment instanceof TaggedDataUploadAttachment)) {
            return INCORRECT_TRANSACTION;
        }

        TaggedDataUploadAttachment myTaggedData = (TaggedDataUploadAttachment) attachment;
        if (!Arrays.equals(myTaggedData.getHash(), taggedData.getHash())) {
            return HASHES_MISMATCH;
        }

        JSONObject response = myTaggedData.getJSONObject();
        response.put("verify", true);
        return response;
    }

}
