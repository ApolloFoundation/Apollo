/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.peer;

import com.apollocurrency.aplwallet.apl.core.http.JettyConnectorCreator;
import com.apollocurrency.aplwallet.apl.core.task.TaskDispatchManager;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.UPnP;
import com.apollocurrency.aplwallet.apl.util.env.MyNetworkInterfaces;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.task.Task;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.DoSFilter;
import org.jboss.weld.environment.servlet.Listener;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.DispatcherType;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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
     boolean shareMyAddress=false;
     private int myPeerServerPort;
     private final int myPeerServerPortTLS;
     private final boolean useTLS;
     boolean enablePeerUPnP;
     private final String myPlatform;
     private PeerAddress myExtAddress;
     private final Server peerServer;
     private PeerServlet peerServlet;
     private final UPnP upnp;
     private final String host;
     private final int idleTimeout;
     private static List<Integer> externalPorts=new ArrayList<>();

     private TaskDispatchManager taskDispatchManager;

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

    public PeerAddress getMyExtAddress(){
        return myExtAddress;
    }
    public PeerServlet getPeerServlet(){
        return peerServlet;
    }

    @Inject
    public PeerHttpServer(PropertiesHolder propertiesHolder, UPnP upnp, JettyConnectorCreator conCreator, TaskDispatchManager taskDispatchManager) {
        this.upnp = upnp;
        this.taskDispatchManager = taskDispatchManager;
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

        //get configured external adderes from config. UPnP should be disabled, in other case
        // UPnP re-writes this
        String myAddress = Convert.emptyToNull(propertiesHolder.getStringProperty("apl.myAddress", "").trim());
        if(myAddress!=null){
            myExtAddress = new PeerAddress(myPeerServerPort, myAddress);
        }
        if (shareMyAddress) {
            peerServer = new Server();

            conCreator.addHttpConnector(host, myPeerServerPort, peerServer, idleTimeout);
            if(useTLS){
                conCreator.addHttpSConnector(host, myPeerServerPort, peerServer, idleTimeout);
            }

            ServletContextHandler ctxHandler = new ServletContextHandler();
            ctxHandler.setContextPath("/");
            //add Weld listener
            ctxHandler.addEventListener(new Listener());
            peerServlet = new PeerServlet();
            ServletHolder peerServletHolder = new ServletHolder(peerServlet);
            ctxHandler.addServlet(peerServletHolder, "/*");
            if (propertiesHolder.getBooleanProperty("apl.enablePeerServerDoSFilter")) {
                FilterHolder dosFilterHolder = ctxHandler.addFilter(DoSFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
                dosFilterHolder.setInitParameter("maxRequestsPerSec", propertiesHolder.getStringProperty("apl.peerServerDoSFilter.maxRequestsPerSec"));
                dosFilterHolder.setInitParameter("delayMs", propertiesHolder.getStringProperty("apl.peerServerDoSFilter.delayMs"));
                dosFilterHolder.setInitParameter("maxRequestMs", propertiesHolder.getStringProperty("apl.peerServerDoSFilter.maxRequestMs"));
                dosFilterHolder.setInitParameter("trackSessions", "false");
                dosFilterHolder.setAsyncSupported(true);
            }
            if (propertiesHolder.getBooleanProperty("apl.enablePeerServerGZIPFilter")) {
                GzipHandler gzipHandler = new GzipHandler();
                gzipHandler.setIncludedMethods("GET", "POST");
                gzipHandler.setIncludedPaths("/*");
                gzipHandler.setMinGzipSize(PeersService.MIN_COMPRESS_SIZE);
                ctxHandler.setGzipHandler(gzipHandler);
            }
            peerServer.setHandler(ctxHandler);
            List<Integer> internalPorts = new ArrayList<>();
                Connector[] peerConnectors = peerServer.getConnectors();
                for (Connector peerConnector : peerConnectors) {
                    if (peerConnector instanceof ServerConnector) {
                       internalPorts.add(((ServerConnector) peerConnector).getPort());
                    }
                }
            //if address is set in config file, we ignore UPnP
            if (enablePeerUPnP && upnp.isAvailable() && myExtAddress==null) {
                for (Integer pn : internalPorts) {
                    int port = upnp.addPort(pn, "Peer2Peer");
                    if (port > 0) {
                        externalPorts.add(port);
                    }
                }
                myExtAddress = new PeerAddress(externalPorts.get(0),upnp.getExternalAddress().getHostAddress());
            }else{
                externalPorts.addAll(internalPorts);
            }
            // if we still do not have addres set in config and do not have UPnP
            //  myExtAddress is still null, do we have public IP?
            if (myExtAddress == null) {
                String addr = getMyPublicIPAdresses();
                if (addr != null) {
                    myExtAddress = new PeerAddress(externalPorts.get(0), addr);
                }
                peerServer.setStopAtShutdown(true);
            }

        } else {
            peerServer = null;
            LOG.info("shareMyAddress is disabled, will not start peer networking server");
        }
    }

    public void start() {
        Task peerUPnPInitTask = Task.builder()
                .name("PeerUPnPInit")
                .task(() -> {
                    try {
                        if (peerServer != null) { // prevent NPE in offLine mode
                            peerServer.start();
                            LOG.info("Started peer networking server at " + host + ":" + myPeerServerPort);
                        } else {
                            LOG.warn("Peer networking server NOT STARTED (offLine mode?)");
                        }
                    } catch (Exception e) {
                        LOG.error("Failed to start peer networking server", e);
                        throw new RuntimeException(e.toString(), e);
                    }
                })
                .build();

        taskDispatchManager.newBackgroundDispatcher("PeerUPnPService")
                .invokeBefore(peerUPnPInitTask);
    }

    public void shutdown(){
        if (peerServer != null) {
            try {
                peerServer.stop();
                if (enablePeerUPnP) {
                    for (int extPort: externalPorts) {
                            upnp.deletePort(extPort);
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
        if(myExtAddress!=null){
            return myExtAddress.getInetAddress();
        }else{
            return null;
        }
    }

    private String getMyPublicIPAdresses(){
        String res=null;
        MyNetworkInterfaces interfaces = new MyNetworkInterfaces();
        List<InetAddress> my_addr = interfaces.getAdressList();
        for(InetAddress a: my_addr){
            if(!(  a.isAnyLocalAddress()
                 ||a.isLinkLocalAddress()
                 ||a.isSiteLocalAddress()
                 ||a.isLoopbackAddress()
                 ||a.isMulticastAddress()
                 ||a instanceof Inet6Address  // it is not completely right, but some nodes have wrong IPv6 settings
              ))
            {
               res=a.getHostAddress();
               break;
            }
        }
        return res;
    }
}
