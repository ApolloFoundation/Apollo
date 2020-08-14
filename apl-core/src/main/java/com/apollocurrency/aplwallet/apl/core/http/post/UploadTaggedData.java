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

import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class UploadTaggedData extends CreateTransaction {

    public UploadTaggedData() {
        super("file", new APITag[]{APITag.DATA, APITag.CREATE_TRANSACTION},
            "name", "description", "tags", "type", "channel", "isText", "filename", "data");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {

        Account account = HttpParameterParserUtil.getSenderAccount(req);
        TaggedDataUploadAttachment taggedDataUploadAttachment = HttpParameterParserUtil.getTaggedData(req);
        return createTransaction(req, account, taggedDataUploadAttachment);

    }

}
