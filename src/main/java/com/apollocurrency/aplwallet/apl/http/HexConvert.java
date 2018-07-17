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

import com.apollocurrency.aplwallet.apl.util.Convert;
import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class HexConvert extends APIServlet.APIRequestHandler {

    private static class HexConvertHolder {
        private static final HexConvert INSTANCE = new HexConvert();
    }

    public static HexConvert getInstance() {
        return HexConvertHolder.INSTANCE;
    }

    private HexConvert() {
        super(new APITag[] {APITag.UTILS}, "string");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        String string = Convert.emptyToNull(req.getParameter("string"));
        if (string == null) {
            return JSON.emptyJSON;
        }
        JSONObject response = new JSONObject();
        try {
            byte[] asHex = Convert.parseHexString(string);
            if (asHex.length > 0) {
                response.put("text", Convert.toString(asHex));
            }
        } catch (RuntimeException ignore) {}
        try {
            byte[] asText = Convert.toBytes(string);
            response.put("binary", Convert.toHexString(asText));
        } catch (RuntimeException ignore) {}
        return response;
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
