/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.AplGlobalObjects;
import org.json.simple.JSONStreamAware;

public class GetUpdateStatus extends APIServlet.APIRequestHandler {

    private static class GetUpdateStatusHolder {
        private static final GetUpdateStatus INSTANCE = new GetUpdateStatus();
    }

    public static GetUpdateStatus getInstance() {
        return GetUpdateStatusHolder.INSTANCE;
    }

    private GetUpdateStatus() {
        super(new APITag[] {APITag.UPDATE});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        return AplGlobalObjects.getUpdaterCore().getUpdateInfo().json();
    }
}
