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
import apl.UpdaterMediator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetUpdateStatus extends APIServlet.APIRequestHandler {

    static final GetUpdateStatus instance = new GetUpdateStatus();

    private GetUpdateStatus() {
        super(new APITag[]{APITag.UPDATE});
    }

    @Override
    protected JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        JSONObject result = new JSONObject();
        UpdaterMediator.UpdateInfo info = UpdaterMediator.getInstance().getUpdateInfo();
        result.put("isUpdate", info.isUpdate());
        if (info.isUpdate()) {
            result.put("level", info.getUpdateLevel());
            result.put("availableVersion", info.getUpdateVersion().toString());
            result.put("estimatedUpdateHeight", info.getUpdateHeight());
            result.put("receivedUpdateHeight", info.getReceivedUpdateHeight());
        }
        return result;
    }
}
