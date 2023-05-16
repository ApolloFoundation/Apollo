/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

public class UnixConfigDirProvider extends DefaultConfigDirProvider {

    public UnixConfigDirProvider(String applicationName, boolean isService, int netIdx, String uuidOrPart) {
        super(applicationName, isService, netIdx, uuidOrPart);
    }

    @Override
    public String getSysConfigLocation() {
        return "/etc/" + applicationName;
    }
}
