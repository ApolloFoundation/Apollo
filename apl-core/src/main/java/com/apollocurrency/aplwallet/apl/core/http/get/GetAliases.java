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

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.core.http.HttpParameterParserUtil;
import com.apollocurrency.aplwallet.apl.core.http.JSONData;
import com.apollocurrency.aplwallet.apl.core.service.state.AliasService;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

@Vetoed
public final class GetAliases extends AbstractAPIRequestHandler {
    private final AliasService aliasService;

    public GetAliases() {
        super(new APITag[]{APITag.ALIASES}, "timestamp", "account", "firstIndex", "lastIndex");
        this.aliasService = CDI.current().select(AliasService.class).get();
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        final int timestamp = HttpParameterParserUtil.getTimestamp(req);
        final long accountId = HttpParameterParserUtil.getAccountId(req, true);
        int firstIndex = HttpParameterParserUtil.getFirstIndex(req);
        int lastIndex = HttpParameterParserUtil.getLastIndex(req);

        JSONArray aliases = new JSONArray();

        aliasService.getAliasesByOwner(accountId, timestamp, firstIndex, lastIndex).stream()
            .map(JSONData::alias)
            .forEach(aliases::add);

        JSONObject response = new JSONObject();
        response.put("aliases", aliases);
        return response;
    }

}
