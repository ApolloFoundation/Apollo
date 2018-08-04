/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import org.apache.commons.lang3.SystemUtils;

public enum Platform {
    LINUX, WINDOWS, OSX;

    public static Platform current() {
        return
                SystemUtils.IS_OS_WINDOWS ? Platform.WINDOWS : // Windows
                        SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX ? Platform.LINUX : // Linux/Unix
                                SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX ? Platform.OSX : // Mac
                                        null;              // Other
    }
}
