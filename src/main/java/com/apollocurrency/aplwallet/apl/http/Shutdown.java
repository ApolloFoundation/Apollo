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

import com.apollocurrency.aplwallet.apl.Apl;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class Shutdown extends APIServlet.APIRequestHandler {

    private static class ShutdownHolder {
        private static final Shutdown INSTANCE = new Shutdown();
    }

    public static Shutdown getInstance() {
        return ShutdownHolder.INSTANCE;
    }

    private Shutdown() {
        super(new APITag[] {APITag.DEBUG}, "scan");
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) {
        JSONObject response = new JSONObject();
        boolean scan = "true".equalsIgnoreCase(req.getParameter("scan"));
        if (scan) {
            Apl.getBlockchainProcessor().fullScanWithShutdown();
        } else {
            new Thread(() -> System.exit(0)).start();
        }
        response.put("shutdown", true);
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
