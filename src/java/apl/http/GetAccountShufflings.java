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

package apl.http;

import apl.Shuffling;
import apl.db.DbIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountShufflings extends APIServlet.APIRequestHandler {

    private static class GetAccountShufflingsHolder {
        private static final GetAccountShufflings INSTANCE = new GetAccountShufflings();
    }

    public static GetAccountShufflings getInstance() {
        return GetAccountShufflingsHolder.INSTANCE;
    }

    private GetAccountShufflings() {
        super(new APITag[] {APITag.SHUFFLING}, "account", "includeFinished", "includeHoldingInfo", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        long accountId = ParameterParser.getAccountId(req, "account", true);
        boolean includeFinished = "true".equalsIgnoreCase(req.getParameter("includeFinished"));
        boolean includeHoldingInfo = "true".equalsIgnoreCase(req.getParameter("includeHoldingInfo"));
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        response.put("shufflings", jsonArray);
        try (DbIterator<Shuffling> shufflings = Shuffling.getAccountShufflings(accountId, includeFinished, firstIndex, lastIndex)) {
            for (Shuffling shuffling : shufflings) {
                jsonArray.add(JSONData.shuffling(shuffling, includeHoldingInfo));
            }
        }
        return response;
    }

}
