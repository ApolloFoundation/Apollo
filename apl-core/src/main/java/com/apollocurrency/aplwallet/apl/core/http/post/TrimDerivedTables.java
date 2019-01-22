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

package com.apollocurrency.aplwallet.apl.core.http.post;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class TrimDerivedTables extends AbstractAPIRequestHandler {

    private static class TrimDerivedTablesHolder {
        private static final TrimDerivedTables INSTANCE = new TrimDerivedTables();
    }

    public static TrimDerivedTables getInstance() {
        return TrimDerivedTablesHolder.INSTANCE;
    }

    private TrimDerivedTables() {
        super(new APITag[] {APITag.DEBUG});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        lookupBlockchainProcessor().trimDerivedTables();
        response.put("done", true);
        return response;
    }

    @Override
    protected final boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }

    @Override
    protected boolean allowRequiredBlockParameters() {
        return false;
    }

    @Override
    protected boolean requireBlockchain() {
        return false;
    }

}
