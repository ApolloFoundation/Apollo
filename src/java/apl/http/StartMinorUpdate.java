/*
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
package apl.http;

import apl.AplException;
import apl.UpdateInfo;
import apl.updater.UpdaterCore;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class StartMinorUpdate extends APIServlet.APIRequestHandler {

    static final StartMinorUpdate instance = new StartMinorUpdate();

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
