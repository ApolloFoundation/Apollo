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

package apl.addons;

import apl.Apl;
import apl.util.Logger;

import java.util.Map;

public final class BeforeShutdown implements AddOn {

    final String beforeShutdownScript = Apl.getStringProperty("apl.beforeShutdownScript");

    @Override
    public void shutdown() {
        if (beforeShutdownScript != null) {
            try {
                Runtime.getRuntime().exec(beforeShutdownScript);
            } catch (Exception e) {
                Logger.logShutdownMessage("Failed to run before shutdown script: " + beforeShutdownScript, e);
            }
        }
    }

    @Override
    public void processRequest(Map<String, String> params) {
        Logger.logInfoMessage("Shutdown:", params.get("shutdownMessage"));
    }
}
