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

package com.apollocurrency.aplwallet.apl.util;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Forward ports using the UPnP protocol.
 */
@Singleton
public class UPnP {
    private static final Logger LOG = getLogger(UPnP.class);

    private boolean isShutdown = false;

    /** UPnP gateway device */
    private GatewayDevice gateway = null;

    /** Local address */
    private  InetAddress localAddress;

    /** External address */
    private InetAddress externalAddress;
    
    public static int TIMEOUT=1500; //1,5 secons


    
//TODO: Inject with properties    
    @Inject  
    public UPnP() {
    }

    /**
     * Add a port to the UPnP mapping
     *
     * @param   port                Port to add
     */
    public synchronized void addPort(int port) {
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
                                       "Apollo")) {
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
    public synchronized void deletePort(int port) {

        try {
            if (gateway != null && gateway.deletePortMapping(port, "TCP")) {
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
    public synchronized InetAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * Return the external address
     *
     * @return  External address or null if the address is not available
     */
    public synchronized InetAddress getExternalAddress() {
        //TODO: set externalAddress from properties if we unable to do UPnP
        return externalAddress;
    }

    /**
     * Initialize the UPnP support
     */
    private void init() {
        AppStatus.getInstance().update("UPnP initialization...");
        //
        // Discover the gateway devices on the local network
        //
        try {
            
            LOG.info("Looking for UPnP gateway device...");
            GatewayDevice.setHttpReadTimeout(TIMEOUT);
            GatewayDiscover discover = new GatewayDiscover();
            discover.setTimeout(TIMEOUT);
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
        AppStatus.getInstance().update("UPnP initialization done.");
    }
}
