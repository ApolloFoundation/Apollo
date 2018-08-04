/*
 * Copyright © 2017-2018 Apollo Foundation
 */
package apl.http;

import apl.AplException;
import apl.UpdateInfo;
import apl.updater.UpdaterCore;
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
        UpdateInfo info = UpdateInfo.getInstance();
        if (info.isStartAllowed()) {
            object.put("updateStarted", "true");
            object.put("updateInfo:", info.json());
            UpdaterCore.getInstance().startUpdate();
        } else {
            object.put("updateStarted", "false");
        }
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
