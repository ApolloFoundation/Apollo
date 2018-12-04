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
import com.apollocurrency.aplwallet.apl.http.APIServlet;
import com.apollocurrency.aplwallet.apl.http.APITag;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public final class AddOns {
    private static final Logger LOG = getLogger(AddOns.class);

    private static final List<AddOn> addOns;
    static {
        List<AddOn> addOnsList = new ArrayList<>();
        Apl.getStringListProperty("apl.addOns").forEach(addOn -> {
            try {
                addOnsList.add((AddOn)Class.forName(addOn).newInstance());
            } catch (ReflectiveOperationException e) {
                LOG.error(e.getMessage(), e);
            }
        });
        addOns = Collections.unmodifiableList(addOnsList);
        if (!addOns.isEmpty() && !Apl.getBooleanProperty("apl.disableSecurityPolicy")) {
            System.setProperty("java.security.policy", Apl.isDesktopApplicationEnabled() ? "apldesktop.policy" : "apl.policy");
            LOG.info("Setting security manager with policy " + System.getProperty("java.security.policy"));
            System.setSecurityManager(new SecurityManager() {
                @Override
                public void checkConnect(String host, int port) {
                    // Allow all connections
                }
                @Override
                public void checkConnect(String host, int port, Object context) {
                    // Allow all connections
                }
                @Override
                public Object getSecurityContext() {
                    return super.getSecurityContext();
                }
            });
        }
        addOns.forEach(addOn -> {
            LOG.info("Initializing " + addOn.getClass().getName());
            addOn.init();
        });
    }

    public static void init() {}

    public static void shutdown() {
        addOns.forEach(addOn -> {
            LOG.info("Shutting down " + addOn.getClass().getName());
            addOn.shutdown();
        });
    }

    public static void registerAPIRequestHandlers(Map<String,APIServlet.APIRequestHandler> map) {
        for (AddOn addOn : addOns) {
            APIServlet.APIRequestHandler requestHandler = addOn.getAPIRequestHandler();
            if (requestHandler != null) {
                if (!requestHandler.getAPITags().contains(APITag.ADDONS)) {
                    LOG.error("Add-on " + addOn.getClass().getName()
                            + " attempted to register request handler which is not tagged as APITag.ADDONS, skipping");
                    continue;
                }
                String requestType = addOn.getAPIRequestType();
                if (requestType == null) {
                    LOG.error("Add-on " + addOn.getClass().getName() + " requestType not defined");
                    continue;
                }
                if (map.get(requestType) != null) {
                    LOG.error("Add-on " + addOn.getClass().getName() + " attempted to override requestType " + requestType + ", skipping");
                    continue;
                }
                LOG.info("Add-on " + addOn.getClass().getName() + " registered new API: " + requestType);
                map.put(requestType, requestHandler);
            }
        }
    }

    private AddOns() {}

}
