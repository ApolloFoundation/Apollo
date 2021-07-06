/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.parser;

import com.apollocurrency.aplwallet.api.p2p.response.GetTransactionsResponse;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;

/**
 * Parser for the {@link com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetTransactions} endpoint response
 * @author Andrii Boiarskyi
 * @see com.apollocurrency.aplwallet.apl.core.peer.endpoint.GetTransactions
 * @since 1.48.4
 */
public class GetTransactionsResponseParser implements JsonReqRespParser<GetTransactionsResponse> {
    @Override
    public GetTransactionsResponse parse(JSONObject json) {
        return JSON.getMapper().convertValue(json, GetTransactionsResponse.class);
    }
}
