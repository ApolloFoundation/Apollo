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
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.util.Filter;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Vetoed
public final class GetExpectedOrderCancellations extends AbstractAPIRequestHandler {


    public GetExpectedOrderCancellations() {
        super(new APITag[]{APITag.AE});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        Filter<Transaction> filter = transaction -> transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.CC_ASK_ORDER_CANCELLATION
            || transaction.getType().getSpec() == TransactionTypes.TransactionTypeSpec.CC_BID_ORDER_CANCELLATION;

        List<? extends Transaction> transactions = lookupBlockchainProcessor().getExpectedTransactions(filter);
        JSONArray cancellations = new JSONArray();
        transactions.forEach(transaction -> cancellations.add(JSONData.expectedOrderCancellation(transaction)));
        JSONObject response = new JSONObject();
        response.put("orderCancellations", cancellations);
        return response;
    }
}
