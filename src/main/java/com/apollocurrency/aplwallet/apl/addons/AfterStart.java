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

package com.apollocurrency.aplwallet.apl.addons;


import com.apollocurrency.aplwallet.apl.Apl;
import com.apollocurrency.aplwallet.apl.util.Logger;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;

import java.util.Map;

public final class AfterStart implements AddOn {

    @Override
    public void init() {
        String afterStartScript = Apl.getStringProperty("apl.afterStartScript");
        if (afterStartScript != null) {
            ThreadPool.runAfterStart(() -> {
                try {
                    Runtime.getRuntime().exec(afterStartScript);
                } catch (Exception e) {
                    Logger.logErrorMessage("Failed to run after start script: " + afterStartScript, e);
                }
            });
        }
    }

    @Override
    public void processRequest(Map<String, String> map) {
        Logger.logInfoMessage(map.get("afterStartMessage"));
    }
}
