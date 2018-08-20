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

import com.apollocurrency.aplwallet.apl.Alias;
import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliasesLike extends APIServlet.APIRequestHandler {

    private static class GetAliasesLikeHolder {
        private static final GetAliasesLike INSTANCE = new GetAliasesLike();
    }

    public static GetAliasesLike getInstance() {
        return GetAliasesLikeHolder.INSTANCE;
    }

    private GetAliasesLike() {
        super(new APITag[] {APITag.ALIASES, APITag.SEARCH}, "aliasPrefix", "firstIndex", "lastIndex");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        String prefix = Convert.emptyToNull(req.getParameter("aliasPrefix"));
        if (prefix == null) {
            return JSONResponses.missing("aliasPrefix");
        }
        if (prefix.length() < 2) {
            return JSONResponses.incorrect("aliasPrefix", "aliasPrefix must be at least 2 characters long");
        }

        JSONObject response = new JSONObject();
        JSONArray aliasJSON = new JSONArray();
        response.put("aliases", aliasJSON);
        try (DbIterator<Alias> aliases = Alias.getAliasesLike(prefix, firstIndex, lastIndex)) {
            while (aliases.hasNext()) {
                aliasJSON.add(JSONData.alias(aliases.next()));
            }
        }
        return response;
    }

}
