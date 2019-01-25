/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

public class UnixConfigDirProvider extends DefaultConfigDirProvider {

    public UnixConfigDirProvider(String applicationName, boolean isService) {
        super(applicationName, isService);
    }

    @Override
    public String getSysConfigDirectory() {
        return "/etc/" + applicationName;
    }
}
