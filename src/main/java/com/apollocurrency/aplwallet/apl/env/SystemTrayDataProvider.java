/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.env;

import java.io.File;
import java.net.URI;

public class SystemTrayDataProvider {

    private final String toolTip;
    private final URI wallet;
    private final File logFile;

    public SystemTrayDataProvider(String toolTip, URI wallet, File logFile) {
        this.toolTip = toolTip;
        this.wallet = wallet;
        this.logFile = logFile;
    }

    public String getToolTip() {
        return toolTip;
    }

    public URI getWallet() {
        return wallet;
    }

    public File getLogFile() {
        return logFile;
    }

    @Override
    public String toString() {
        return "SystemTrayDataProvider{" +
                "toolTip='" + toolTip + '\'' +
                ", wallet=" + wallet +
                ", logFile=" + logFile +
                '}';
    }
}
