/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida IP B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package apl.http;

import apl.AplException;
import apl.UpdateInfo;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

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
        return UpdateInfo.getInstance().json();
    }
}
