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

import com.apollocurrency.aplwallet.apl.AplException;
import com.apollocurrency.aplwallet.apl.TaggedData;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDataTagCount extends APIServlet.APIRequestHandler {

    private static class GetDataTagCountHolder {
        private static final GetDataTagCount INSTANCE = new GetDataTagCount();
    }

    public static GetDataTagCount getInstance() {
        return GetDataTagCountHolder.INSTANCE;
    }

    private GetDataTagCount() {
        super(new APITag[] {APITag.DATA});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        JSONObject response = new JSONObject();
        response.put("numberOfDataTags", TaggedData.Tag.getTagCount());
        return response;
    }

}
