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

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.TaggedData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.Data;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUpload;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static com.apollocurrency.aplwallet.apl.core.http.JSONResponses.UNKNOWN_TRANSACTION;
import javax.enterprise.inject.Vetoed;

@Vetoed
public final class ExtendTaggedData extends CreateTransaction {

    public ExtendTaggedData() {
        super("file", new APITag[] {APITag.DATA, APITag.CREATE_TRANSACTION}, "transaction",
                "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account account = ParameterParser.getSenderAccount(req);
        long transactionId = ParameterParser.getUnsignedLong(req, "transaction", true);
        TaggedData taggedData = TaggedData.getData(transactionId);
        if (taggedData == null) {
            Transaction transaction = lookupBlockchain().getTransaction(transactionId);
            if (transaction == null || transaction.getType() != Data.TAGGED_DATA_UPLOAD) {
                return UNKNOWN_TRANSACTION;
            }
            TaggedDataUpload taggedDataUpload = ParameterParser.getTaggedData(req);
            taggedData = new TaggedData(transaction, taggedDataUpload,
                    lookupBlockchain().getLastBlockTimestamp(),
                    lookupBlockchain().getHeight());
        }
        TaggedDataExtend taggedDataExtend = new TaggedDataExtend(taggedData);
        return createTransaction(req, account, taggedDataExtend);

    }

}
