/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.http.get;

import com.apollocurrency.aplwallet.apl.core.http.APITag;
import com.apollocurrency.aplwallet.apl.core.http.AbstractAPIRequestHandler;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONStreamAware;

import jakarta.enterprise.inject.Vetoed;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.servlet.http.HttpServletRequest;

@Vetoed
public class GetUpdateStatus extends AbstractAPIRequestHandler {

    public GetUpdateStatus() {
        super(new APITag[]{APITag.UPDATE});
    }

    @Override
    public JSONStreamAware processRequest(HttpServletRequest request) throws AplException {
        return CDI.current().select(UpdaterCore.class).get().getUpdateInfo().json();
    }
}
