/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import javax.enterprise.inject.spi.CDI;
import javax.servlet.http.HttpServletRequest;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONStreamAware;

public class GetUpdateStatus extends AbstractAPIRequestHandler {

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
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        return CDI.current().select(UpdaterCore.class).get().getUpdateInfo().json();
    }
}
