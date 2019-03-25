/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.util.EnumSet;
import javax.enterprise.inject.Vetoed;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Peer HTTP server that handles http requests and PeerWebSockets
 * @author alukin@gmail.com
 */
@Singleton
public class PeerHttpServer {
    
     private static final Logger LOG = getLogger(PeerHttpServer.class);
     
     static final int MAX_PLATFORM_LENGTH = 30;
    
     private PropertiesHolder propertiesHolder;
     boolean shareMyAddress;
     int myPeerServerPort;
     boolean enablePeerUPnP;    
     String myPlatform;
     String myAddress;
     Server peerServer;
     UPnP upnp;
    
    @Inject
    public PeerHttpServer(PropertiesHolder propertiesHolder, UPnP upnp) {
        this.propertiesHolder = propertiesHolder;
        this.upnp = upnp;
        shareMyAddress = propertiesHolder.getBooleanProperty("apl.shareMyAddress") && ! propertiesHolder.isOffline();  
        myPeerServerPort = propertiesHolder.getIntProperty("apl.myPeerServerPort");
        String platform = propertiesHolder.getStringProperty("apl.myPlatform", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        if (platform.length() > MAX_PLATFORM_LENGTH) {
            platform = platform.substring(0, MAX_PLATFORM_LENGTH);
        }
        enablePeerUPnP = propertiesHolder.getBooleanProperty("apl.enablePeerUPnP");
        
        myPlatform = platform;
        myAddress = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.myAddress", "").trim());      
        Peers.shutdown = false;
        if (shareMyAddress) {
            peerServer = new Server();
            ServerConnector connector = new ServerConnector(peerServer);
            final int port = myPeerServerPort;
            connector.setPort(port);
            final String host = propertiesHolder.getStringProperty("apl.peerServerHost");
            connector.setHost(host);
            connector.setIdleTimeout(propertiesHolder.getIntProperty("apl.peerServerIdleTimeout"));
            connector.setReuseAddress(true);
            peerServer.addConnector(connector);
            ServletContextHandler ctxHandler = new ServletContextHandler();
            ctxHandler.setContextPath("/");
            ServletHolder peerServletHolder = new ServletHolder(new PeerServlet());
            ctxHandler.addServlet(peerServletHolder, "/*");
            if (propertiesHolder.getBooleanProperty("apl.enablePeerServerDoSFilter")) {
                FilterHolder dosFilterHolder = ctxHandler.addFilter(DoSFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
                dosFilterHolder.setInitParameter("maxRequestsPerSec", propertiesHolder.getStringProperty("apl.peerServerDoSFilter.maxRequestsPerSec"));
                dosFilterHolder.setInitParameter("delayMs", propertiesHolder.getStringProperty("apl.peerServerDoSFilter.delayMs"));
                dosFilterHolder.setInitParameter("maxRequestMs", propertiesHolder.getStringProperty("apl.peerServerDoSFilter.maxRequestMs"));
                dosFilterHolder.setInitParameter("trackSessions", "false");
                dosFilterHolder.setAsyncSupported(true);
            }
            if (Peers.isGzipEnabled) {
                GzipHandler gzipHandler = new GzipHandler();
                gzipHandler.setIncludedMethods("GET", "POST");
                gzipHandler.setIncludedPaths("/*");
                gzipHandler.setMinGzipSize(Peers.MIN_COMPRESS_SIZE);
                ctxHandler.setGzipHandler(gzipHandler);
            }
            peerServer.setHandler(ctxHandler);
            peerServer.setStopAtShutdown(true);
            
            ThreadPool.runBeforeStart("PeerUPnPInit", () -> {
                try {
                    if (enablePeerUPnP) {
                        Connector[] peerConnectors = peerServer.getConnectors();
                        for (Connector peerConnector : peerConnectors) {
                            if (peerConnector instanceof ServerConnector) {
                                upnp.addPort(((ServerConnector) peerConnector).getPort());
                            }
                        }
                    }
                    peerServer.start();
                    LOG.info("Started peer networking server at " + host + ":" + port);
                } catch (Exception e) {
                    LOG.error("Failed to start peer networking server", e);
                    throw new RuntimeException(e.toString(), e);
                }
            }, true);
        } else {
            peerServer = null;
            LOG.info("shareMyAddress is disabled, will not start peer networking server");
        }
    }
    
    public void shutdown(){
        if (peerServer != null) {
            try {
                peerServer.stop();
                if (enablePeerUPnP) {
                    Connector[] peerConnectors = peerServer.getConnectors();
                    for (Connector peerConnector : peerConnectors) {
                        if (peerConnector instanceof ServerConnector)
                            upnp.deletePort(((ServerConnector)peerConnector).getPort());
                    }
                }
            } catch (Exception e) {
                LOG.info("Failed to stop peer server", e);
            }
        }        
    }
    
    public boolean suspend(){
         boolean res = false;
           if (peerServer != null) {
            try {
                peerServer.stop();
                res=true;
            } catch (Exception e) {
                LOG.info("Failed to stop peer server", e);
            }
        }  
        return res;
    }
    
    public  boolean resume() {
        boolean res = false;
        if (peerServer != null) {
            try {
                LOG.debug("Starting peer server");
                peerServer.start();
                LOG.debug("peer server started");
                res = true;
            } catch (Exception e) {
                LOG.info("Failed to resume peer server", e);
            }
        }
        return res;
    }    
}
