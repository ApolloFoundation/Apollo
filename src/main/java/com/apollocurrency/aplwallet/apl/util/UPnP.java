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

package com.apollocurrency.aplwallet.apl.util;

import com.apollocurrency.aplwallet.apl.Apl;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Forward ports using the UPnP protocol.
 */
public class UPnP {
    private static final Logger LOG = getLogger(UPnP.class);

    /** Initialization done */
    private static boolean initDone = false;

    private static boolean isShutdown = false;

    /** UPnP gateway device */
    private static GatewayDevice gateway = null;

    /** Local address */
    private static InetAddress localAddress;

    /** External address */
    private static InetAddress externalAddress;

    /**
     * Add a port to the UPnP mapping
     *
     * @param   port                Port to add
     */
    public static synchronized void addPort(int port) {
        if (!initDone)
            init();
        //
        // Ignore the request if we didn't find a gateway device
        //
        if (gateway == null)
            return;
        //
        // Forward the port
        //
        try {
            if (gateway.addPortMapping(port, port, localAddress.getHostAddress(), "TCP",
                                       Apl.APPLICATION + " " + Apl.VERSION)) {
                LOG.debug("Mapped port [" + externalAddress.getHostAddress() + "]:" + port);
            } else {
                LOG.debug("Unable to map port " + port);
            }
        } catch (Exception exc) {
            LOG.error("Unable to map port " + port + ": " + exc.toString());
        }
    }

    /**
     * Delete a port from the UPnP mapping
     *
     * @param   port                Port to delete
     */
    public static synchronized void deletePort(int port) {
        if (!initDone || gateway == null)
            return;
        //
        // Delete the port
        //
        try {
            if (gateway.deletePortMapping(port, "TCP")) {
                LOG.debug("Mapping deleted for port " + port);
            } else {
                LOG.debug("Unable to delete mapping for port " + port);
            }
        } catch (Exception exc) {
            LOG.error("Unable to delete mapping for port " + port + ": " + exc.toString());
        }
    }

    /**
     * Return the local address
     *
     * @return                      Local address or null if the address is not available
     */
    public static synchronized InetAddress getLocalAddress() {
        if (!initDone)
            init();
        return localAddress;
    }

    /**
     * Return the external address
     *
     * @return                      External address or null if the address is not available
     */
    public static synchronized InetAddress getExternalAddress() {
        if (!initDone)
            init();
        return externalAddress;
    }

    /**
     * Initialize the UPnP support
     */
    private static void init() {
        initDone = true;
        Apl.getRuntimeMode().updateAppStatus("UPnP initialization...");
        //
        // Discover the gateway devices on the local network
        //
        try {
            LOG.info("Looking for UPnP gateway device...");
            GatewayDevice.setHttpReadTimeout(Apl.getIntProperty("apl.upnpGatewayTimeout", GatewayDevice.getHttpReadTimeout()));
            GatewayDiscover discover = new GatewayDiscover();
            discover.setTimeout(Apl.getIntProperty("apl.upnpDiscoverTimeout", discover.getTimeout()));
            Map<InetAddress, GatewayDevice> gatewayMap = discover.discover();
            if (gatewayMap == null || gatewayMap.isEmpty()) {
                LOG.debug("There are no UPnP gateway devices");
            } else {
                gatewayMap.forEach((addr, device) ->
                        LOG.debug("UPnP gateway device found on " + addr.getHostAddress()));
                gateway = discover.getValidGateway();
                if (gateway == null) {
                    LOG.debug("There is no connected UPnP gateway device");
                } else {
                    localAddress = gateway.getLocalAddress();
                    externalAddress = InetAddress.getByName(gateway.getExternalIPAddress());
                    LOG.debug("Using UPnP gateway device on " + localAddress.getHostAddress());
                    LOG.info("External IP address is " + externalAddress.getHostAddress());
                }
            }
        } catch (Exception exc) {
            LOG.error("Unable to discover UPnP gateway devices: " + exc.toString());
        }
    }
}
