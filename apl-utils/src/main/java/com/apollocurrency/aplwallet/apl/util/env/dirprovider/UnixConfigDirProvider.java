/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

public class UnixConfigDirProvider extends DefaultConfigDirProvider {

    public UnixConfigDirProvider(String applicationName, boolean isService, int netIdx, String uuid_or_part) {
        super(applicationName, isService, netIdx, uuid_or_part);
    }

    @Override
    public String getSysConfigLocation() {
        return "/etc/" + applicationName;
    }
}
