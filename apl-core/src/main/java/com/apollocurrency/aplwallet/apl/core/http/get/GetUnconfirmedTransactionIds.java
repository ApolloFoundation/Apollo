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

import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Vetoed
public final class GetUnconfirmedTransactionIds extends AbstractAPIRequestHandler {

    public GetUnconfirmedTransactionIds() {
        super(new APITag[]{APITag.TRANSACTIONS, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Set<Long> accountIds = Convert.toSet(HttpParameterParserUtil.getAccountIds(req, false));
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONArray transactionIds = new JSONArray();
        if (accountIds.isEmpty()) {
            for (UnconfirmedTransaction tx : lookupMemPool().getProcessed(firstIndex, lastIndex)) {
                transactionIds.add(tx.getStringId());
            }
        } else {
            int limit = DbUtils.calculateLimit(firstIndex, lastIndex);
            if (limit == 0) {
                limit = Integer.MAX_VALUE;
            }
                CollectionUtil.forEach(lookupMemPool().getAllProcessedStream()
                .filter(transaction -> accountIds.contains(transaction.getSenderId()) || accountIds.contains(transaction.getRecipientId()))
                .limit(limit)
                .skip(firstIndex), e-> transactionIds.add(e.getStringId()));
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
