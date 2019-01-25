/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.io.File;
import java.util.UUID;

public class UserModeDirProvider extends AbstractDirProvider {

    private static final String USER_HOME = System.getProperty("user.home");

    public UserModeDirProvider(String applicationName, UUID chainId) {
        super(USER_HOME + File.separator + "." + applicationName, applicationName, chainId);
    }

    public UserModeDirProvider(String applicationName, UUID chainId, PredefinedDirLocations dirLocations) {
        super(USER_HOME + File.separator + "." + applicationName, applicationName, chainId, dirLocations);
    }
}
