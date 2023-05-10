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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.addons;


import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.slf4j.Logger;

import jakarta.enterprise.inject.spi.CDI;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public final class BeforeShutdown implements AddOn {
    private static final Logger LOG = getLogger(BeforeShutdown.class);

    // TODO: YL put into constructor
    private static PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();


    final String beforeShutdownScript = propertiesHolder.getStringProperty("apl.beforeShutdownScript");

    @Override
    public void shutdown() {
        if (beforeShutdownScript != null) {
            try {
                Runtime.getRuntime().exec(beforeShutdownScript);
            } catch (Exception e) {
                LOG.info("Failed to run before shutdown script: " + beforeShutdownScript, e);
            }
        }
    }

    @Override
    public void processRequest(Map<String, String> params) {
        LOG.info("Shutdown:", params.get("shutdownMessage"));
    }
}
