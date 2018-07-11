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

public enum Platform {
    LINUX, WINDOWS, OSX;

    public static Platform current() {
        return
               SystemUtils.IS_OS_WINDOWS                          ? Platform.WINDOWS : // Windows
               SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_UNIX  ? Platform.LINUX   : // Linux/Unix
               SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX ? Platform.OSX     : // Mac
                                                                    null;              // Other
    }
}
