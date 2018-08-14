/*
 * Copyright © 2017-2018 Apollo Foundation
 */

package apl.updater;

import org.apache.commons.lang3.SystemUtils;

public enum Architecture {
    X86, AMD64, ARM;

    public static Architecture current() {
        String osArch = SystemUtils.OS_ARCH.toLowerCase();
        switch (osArch) {
            case "x86_64":
            case "amd64":
                return AMD64;
            case "x86":
                return X86;
        }
        if (osArch.startsWith("arm")) {
            return ARM;
        }
        return null;
    }
}
