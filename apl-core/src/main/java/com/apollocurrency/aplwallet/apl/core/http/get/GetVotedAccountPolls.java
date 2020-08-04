/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.utils.CollectorUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public class GetVotedAccountPolls extends AbstractAPIRequestHandler {
    public GetVotedAccountPolls() {
        super(new APITag[]{APITag.VS, APITag.ACCOUNTS}, "account", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        long account = HttpParameterParserUtil.getAccountId(request, true);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(request);
        int lastIndex = HttpParameterParserUtil.getLastIndex(request);
        JSONObject response = new JSONObject();
        JSONArray pollJsonArray = pollService.getVotedPollsByAccount(account, firstIndex, lastIndex)
            .map(JSONData::poll)
            .collect(CollectorUtils.jsonCollector());

        response.put("polls", pollJsonArray);
        return response;
    }
}
