/*
 * Copyright © 2017-2018 Apollo Foundation
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Apollo Foundation,
 * no part of the Apl software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

package com.apollocurrency.aplwallet.apl.updater;

import org.apache.commons.lang3.SystemUtils;

public enum Architecture {
    X86, AMD64, ARM;

    public static Architecture current() {
        String osArch = SystemUtils.OS_ARCH.toLowerCase();
        switch (osArch) {
            case "x86_64":
            case "amd64" :
                return AMD64;
            case "x86" :
                return X86;
        }
        if (osArch.startsWith("arm")) {
            return ARM;
        }
        return null;
    }
}
