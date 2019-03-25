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
import com.apollocurrency.aplwallet.apl.core.http.ParameterParser;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.db.FilteringIterator;
import javax.enterprise.inject.Vetoed;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAliases extends AbstractAPIRequestHandler {

    public GetAliases() {
        super(new APITag[] {APITag.ALIASES}, "timestamp", "account", "firstIndex", "lastIndex");
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        final int timestamp = ParameterParser.getTimestamp(req);
        final long accountId = ParameterParser.getAccountId(req, true);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray aliases = new JSONArray();
        try (FilteringIterator<Alias> aliasIterator = new FilteringIterator<>(Alias.getAliasesByOwner(accountId, 0, -1),
                alias -> alias.getTimestamp() >= timestamp, firstIndex, lastIndex)) {
            while(aliasIterator.hasNext()) {
                aliases.add(JSONData.alias(aliasIterator.next()));
            }
        }

        JSONObject response = new JSONObject();
        response.put("aliases", aliases);
        return response;
    }

}
