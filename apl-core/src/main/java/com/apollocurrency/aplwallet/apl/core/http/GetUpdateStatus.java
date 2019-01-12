/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.util.AplException;
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
        return CDI.current().select(UpdaterCore.class).get().getUpdateInfo().json();
    }
}
