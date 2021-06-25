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

import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.ParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Vetoed
public final class GetUnconfirmedTransactions extends AbstractAPIRequestHandler {

    public GetUnconfirmedTransactions() {
        super(new APITag[]{APITag.TRANSACTIONS, APITag.ACCOUNTS}, "account", "account", "account", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        Set<Long> accountIds = Convert.toSet(HttpParameterParserUtil.getAccountIds(req, false));
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONArray transactions = new JSONArray();
        int limit = DbUtils.calculateLimit(firstIndex, lastIndex);
        if (limit == 0) {
            limit = Integer.MAX_VALUE;
        }
        if (accountIds.isEmpty()) {
            CollectionUtil.forEach(lookupMemPool().getAllStream()
                .filter(transaction -> transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT)
                .skip(firstIndex)
                .limit(limit),e -> transactions.add(JSONData.unconfirmedTransaction(e)));
        } else {
            CollectionUtil.forEach(lookupMemPool().getAllStream()
                .filter(transaction -> transaction.getType().getSpec() != TransactionTypes.TransactionTypeSpec.PRIVATE_PAYMENT
                    && (accountIds.contains(transaction.getSenderId()) || accountIds.contains(transaction.getRecipientId())))
                .skip(firstIndex)
                .limit(limit), e -> transactions.add(JSONData.unconfirmedTransaction(e)));
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactions", transactions);
        return response;
    }

}
