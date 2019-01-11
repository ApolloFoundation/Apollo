/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.http;

import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.core.app.AplGlobalObjects;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class StartAvailableUpdate extends APIServlet.APIRequestHandler {

    private static class StartAvailableUpdateHolder {
        private static final StartAvailableUpdate INSTANCE = new StartAvailableUpdate();
    }

    public static StartAvailableUpdate getInstance() {
        return StartAvailableUpdateHolder.INSTANCE;
    }

    private StartAvailableUpdate() {
        super(new APITag[] {APITag.UPDATE});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        JSONObject object = new JSONObject();
        boolean started = AplGlobalObjects.getUpdaterCore().startAvailableUpdate();
        object.put("updateStarted", started);
        object.put("updateInfo", AplGlobalObjects.getUpdaterCore().getUpdateInfo().json());
        return object;
    }

    @Override
    protected boolean requireFullClient() {
        return true;
    }

    @Override
    protected boolean requirePost() {
        return true;
    }

    @Override
    protected boolean requirePassword() {
        return true;
    }
}
