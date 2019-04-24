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

import java.io.IOException;
import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.slf4j.Logger;

import java.net.InetAddress;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.parsers.ParserConfigurationException;
import org.bitlet.weupnp.PortMappingEntry;

import static org.slf4j.LoggerFactory.getLogger;
import org.xml.sax.SAXException;

/**
 * Forward ports using the UPnP protocol.
 */
@Singleton
public class UPnP {

    private static final Logger LOG = getLogger(UPnP.class);
    public static final int MAX_PORTS_TO_TRY=999;
    private boolean isShutdown = false;

    /**
     * UPnP gateway device
     */
    private GatewayDevice gateway = null;

    /**
     * Local address
     */
    private InetAddress localAddress;

    /**
     * External address
     */
    private InetAddress externalAddress;

    public static int TIMEOUT = 1500; //1,5 secons
    private boolean inited = false;

//TODO: Inject with properties    
    @Inject
    public UPnP() {
    }

    public boolean isInited() {
        return inited;
    }

    public int getFreePort(int desiredExternalPort) throws IOException, SAXException {
        int port = desiredExternalPort;
        PortMappingEntry portMappingEntry = new PortMappingEntry();
        boolean busy = true;
        int count =0;
        while(busy){
         busy = gateway.getSpecificPortMappingEntry(port, "TCP", portMappingEntry);
         if(busy){
             String mapAddr = portMappingEntry.getInternalClient();
             String myAddr = localAddress.getHostAddress();
             if(mapAddr.equalsIgnoreCase(myAddr)){
                 //it is my mapping lost somehow
                 break;
             }             
             port++;
             count++;
             if(count>MAX_PORTS_TO_TRY){
                 port=-1;
                 break;
             }
         }
        }
        return port;
    }

    /**
     * Add a port to the UPnP mapping
     *
     * @param localPort Port to add
     * @param description Description of port mapping
     * @return assigned external port number or -1 if no success
     */
    public synchronized int addPort(int localPort, String description) {
        int externalPort = -1;
        //
        // Ignore the request if we didn't find a gateway device
        //
        if (gateway == null) {
            return externalPort;
        }
        //
        // Forward the port
        //
        try {
            Integer extPortRQ = getFreePort(localPort);
            if (extPortRQ < 0) {
                return externalPort;
            }

            if (gateway.addPortMapping(extPortRQ, localPort, localAddress.getHostAddress(), "TCP",
                    "Apollo(" + localAddress.getHostAddress() + "): " + description)) {
                LOG.debug("Mapped port [" + externalAddress.getHostAddress() + "]:" + localPort
                        + " to: " + externalAddress.getHostAddress() + ":" + extPortRQ.toString());
                externalPort = extPortRQ;
            } else {
                LOG.debug("Unable to map port " + localPort);
            }
        } catch (IOException | SAXException exc) {
            LOG.error("Unable to map port " + localPort + ": " + exc.toString());
        }
        return externalPort;
    }

    /**
     * Delete a port from the UPnP mapping
     *
     * @param externalPort EXTERNAL port to delete
     */
    public synchronized void deletePort(int externalPort) {

        try {
            if (gateway != null && gateway.deletePortMapping(externalPort, "TCP")) {
                LOG.debug("Mapping deleted for port " + externalPort);
            } else {
                LOG.debug("Unable to delete mapping for port " + externalPort);
            }
        } catch (IOException | SAXException exc) {
            LOG.error("Unable to delete mapping for port " + externalPort + ": " + exc.toString());
        }
    }

    /**
     * Return the local address
     *
     * @return Local address or null if the address is not available
     */
    public synchronized InetAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * Return the external address
     *
     * @return External address or null if the address is not available
     */
    public synchronized InetAddress getExternalAddress() {
        //TODO: set externalAddress from properties if we unable to do UPnP
        return externalAddress;
    }

    /**
     * Initialize the UPnP support
     */
    public void init() {
        AppStatus.getInstance().update("UPnP initialization...");
        //
        // Discover the gateway devices on the local network
        //
        inited = true;
        try {

            LOG.info("Looking for UPnP gateway device...");
            GatewayDevice.setHttpReadTimeout(TIMEOUT);
            GatewayDiscover discover = new GatewayDiscover();
            discover.setTimeout(TIMEOUT);
            Map<InetAddress, GatewayDevice> gatewayMap = discover.discover();
            if (gatewayMap == null || gatewayMap.isEmpty()) {
                LOG.debug("There are no UPnP gateway devices");
            } else {
                gatewayMap.forEach((addr, device)
                        -> LOG.debug("UPnP gateway device found on " + addr.getHostAddress()));
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
        } catch (IOException | ParserConfigurationException | SAXException exc) {
            LOG.error("Unable to discover UPnP gateway devices: " + exc.toString());
        }
        AppStatus.getInstance().update("UPnP initialization done.");
    }
}
