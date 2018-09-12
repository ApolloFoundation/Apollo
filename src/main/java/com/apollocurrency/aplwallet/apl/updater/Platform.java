/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.updater;

import org.apache.commons.lang3.SystemUtils;

public enum Platform {
    LINUX, WINDOWS, MAC_OS;

    public static Platform current() {
        return
               SystemUtils.IS_OS_WINDOWS ? Platform.WINDOWS : // Windows
               SystemUtils.IS_OS_LINUX  ? Platform.LINUX   : // Linux
               SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX ? Platform.MAC_OS : // Mac
                                                                    null;              // Other
    }
}
