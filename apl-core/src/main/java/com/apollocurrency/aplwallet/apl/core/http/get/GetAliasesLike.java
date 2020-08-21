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

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.http.JSONResponses;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAliasesLike extends AbstractAPIRequestHandler {
    private final AliasService aliasService;

    public GetAliasesLike() {
        super(new APITag[]{APITag.ALIASES, APITag.SEARCH}, "aliasPrefix", "firstIndex", "lastIndex");
        this.aliasService = CDI.current().select(AliasService.class).get();
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);
        String prefix = Convert.emptyToNull(req.getParameter("aliasPrefix"));
        if (prefix == null) {
            return JSONResponses.missing("aliasPrefix");
        }
        if (prefix.length() < 2) {
            return JSONResponses.incorrect("aliasPrefix", "aliasPrefix must be at least 2 characters long");
        }

        JSONObject response = new JSONObject();
        final JSONArray aliasJSON = new JSONArray();

        aliasService.getAliasesByNamePattern(prefix, firstIndex, lastIndex)
            .stream()
            .map(JSONData::alias)
            .forEach(aliasJSON::add);

        response.put("aliases", aliasJSON);
        return response;
    }

}
