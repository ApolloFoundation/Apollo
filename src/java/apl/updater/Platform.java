package apl.updater;

import org.apache.commons.lang3.SystemUtils;

public enum Platform {
    LINUX, WINDOWS, OSX;

    public static Platform current() {
        return SystemUtils.IS_OS_WINDOWS ? Platform.WINDOWS : SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX ? Platform.LINUX : SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX ? Platform.OSX : null;
    }
}
