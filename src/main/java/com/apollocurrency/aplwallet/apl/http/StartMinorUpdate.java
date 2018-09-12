/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.http;

import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.AplException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class StartMinorUpdate extends APIServlet.APIRequestHandler {

    private static class StartMinorUpdateHolder {
        private static final StartMinorUpdate INSTANCE = new StartMinorUpdate();
    }

    public static StartMinorUpdate getInstance() {
        return StartMinorUpdateHolder.INSTANCE;
    }

    private StartMinorUpdate() {
        super(new APITag[] {APITag.UPDATE});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest req) throws AplException {
        JSONObject object = new JSONObject();
        boolean started = Apl.startMinorUpdate();
        object.put("updateStarted", started);
        object.put("updateInfo", Apl.getUpdateInfo().json());
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
