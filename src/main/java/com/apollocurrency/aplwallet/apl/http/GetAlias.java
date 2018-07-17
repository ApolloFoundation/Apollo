/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Alias;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAlias extends APIServlet.APIRequestHandler {

    private static class GetAliasHolder {
        private static final GetAlias INSTANCE = new GetAlias();
    }

    public static GetAlias getInstance() {
        return GetAliasHolder.INSTANCE;
    }

    private GetAlias() {
        super(new APITag[] {APITag.ALIASES}, "alias", "aliasName");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
        Alias alias = ParameterParser.getAlias(req);
        return JSONData.alias(alias);
    }

}
