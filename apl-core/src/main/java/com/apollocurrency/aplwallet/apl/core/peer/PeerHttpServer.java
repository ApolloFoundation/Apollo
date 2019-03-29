/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.http.JettyConnectorCreator;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.ThreadPool;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.net.InetAddress;
import java.util.EnumSet;
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
     
     public static final int MAX_PLATFORM_LENGTH = 30;
     public static final int DEFAULT_PEER_PORT=47874;
     public static final int DEFAULT_PEER_PORT_TLS=48743;
     boolean shareMyAddress;
     private final int myPeerServerPort;
     private final int myPeerServerPortTLS;
     private final boolean useTLS;
     boolean enablePeerUPnP;    
     private final String myPlatform;
     private final String myAddress;
     private final Server peerServer;
     private final UPnP upnp;
     private final String host;
     private final int idleTimeout; 
     
    public boolean isShareMyAddress() {
        return shareMyAddress;
    }

    public int getMyPeerServerPort() {
        return myPeerServerPort;
    }

    public int getMyPeerServerPortTLS() {
        return myPeerServerPortTLS;
    }

    public String getMyPlatform() {
        return myPlatform;
    }

    public String getMyAddress() {
        return myAddress;
    }

    
    @Inject
    public PeerHttpServer(PropertiesHolder propertiesHolder, UPnP upnp, JettyConnectorCreator conCreator) {
        this.upnp = upnp;
        shareMyAddress = propertiesHolder.getBooleanProperty("apl.shareMyAddress") && ! propertiesHolder.isOffline();  
        myPeerServerPort = propertiesHolder.getIntProperty("apl.myPeerServerPort",DEFAULT_PEER_PORT);
        myPeerServerPortTLS = propertiesHolder.getIntProperty("apl.myPeerServerPortTLS", DEFAULT_PEER_PORT_TLS);
        useTLS=propertiesHolder.getBooleanProperty("apl.peerUseTLS");
        host = propertiesHolder.getStringProperty("apl.peerServerHost");
        String platform = propertiesHolder.getStringProperty("apl.myPlatform", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        if (platform.length() > MAX_PLATFORM_LENGTH) {
            platform = platform.substring(0, MAX_PLATFORM_LENGTH);
        }        
        myPlatform = platform;
        
        enablePeerUPnP = propertiesHolder.getBooleanProperty("apl.enablePeerUPnP");
        idleTimeout = propertiesHolder.getIntProperty("apl.peerServerIdleTimeout");

        myAddress = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.myAddress", "").trim());      
 
        if (shareMyAddress) {
            peerServer = new Server();
            
            conCreator.addHttpConnector(host, myPeerServerPort, peerServer, idleTimeout);
            if(useTLS){
                conCreator.addHttpSConnector(host, myPeerServerPort, peerServer, idleTimeout);
            }
            
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
            

        } else {
            peerServer = null;
            LOG.info("shareMyAddress is disabled, will not start peer networking server");
        }
    }
    
    public void start(){
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
                    LOG.info("Started peer networking server at " + host + ":" + myPeerServerPort);
                } catch (Exception e) {
                    LOG.error("Failed to start peer networking server", e);
                    throw new RuntimeException(e.toString(), e);
                }
            }, true);     
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

    public InetAddress getExternalAddress() {
        return upnp.getExternalAddress();
    }
}
