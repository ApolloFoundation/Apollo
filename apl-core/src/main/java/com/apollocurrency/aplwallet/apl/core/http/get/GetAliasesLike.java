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

import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAliasesLike extends AbstractAPIRequestHandler {

    public GetAliasesLike() {
        super(new APITag[] {APITag.ALIASES, APITag.SEARCH}, "aliasPrefix", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
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
