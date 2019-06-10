/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.transport.TransportEventDescriptor;
import com.apollocurrency.aplwallet.api.transport.TransportStartRequest;
import com.apollocurrency.aplwallet.api.transport.TransportStatusReply;
import com.apollocurrency.aplwallet.api.transport.TransportStopRequest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Random;

import lombok.Getter;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;


@WebSocket(maxTextMessageSize = 64 * 1024)
public class TransportInteractionWebSocket {
    
    
    private String serversJSON = "./servers.json";
    private boolean shuffle = true, uniqueEnable = false; 
    private int uniquePort = -1; 
    private String logPath = "/var/log";   
        
    @SuppressWarnings("unused")
    private Session session;    
    
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionWebSocket.class);
    
    private SecureTransportStatus secureTransportStatus;
    
    // status of the transport connection
    public enum SecureTransportStatus implements Serializable {
        INITIAL, NOT_LAUNCHED, PENDING, CONNECTED, DISCONNECTED;
        
        public String toString(){
            switch(this) {
                case INITIAL        : return "INITIAL";
                case NOT_LAUNCHED   : return "NOT_LAUNCHED";
                case PENDING        : return "PENDING";
                case CONNECTED      : return "CONNECTED";
                case DISCONNECTED   : return "DISCONNECTED";
            };
        return null;
        }                
    }
    
    @Getter
    String remoteip;   
    @Getter
    int remoteport;    
    @Getter
    String tunaddr;
    @Getter
    String tunnetmask;  
    
    
    private WebSocketClient client;    
    private CountDownLatch closeLatch ;
    
    
    /**
     * asynchronous routine to cope with websockets
     *
     * @param duration - time of expectation in units
     * @param unit - TimeUnit description for async events
     */
    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }
      
    
    /**
     * entry point for our interaction routine
     *
     * @param endpointURI - link to connect to transport daemon
     */
    public TransportInteractionWebSocket(URI endpointURI) {
        
        try {

        secureTransportStatus = SecureTransportStatus.INITIAL;
        cleanupComParams();

        this.closeLatch = new CountDownLatch(1);
        client = new WebSocketClient();
        client.start();
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        client.connect(this, endpointURI, request);
        log.debug("Connecting to : %s%n", endpointURI);
        awaitClose(5, TimeUnit.SECONDS);
        
        } catch (Exception ex) {
            log.debug("WS connection exception: " +  ex.getMessage().toString());
        }
        
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnWebSocketConnect
    public void onOpen(Session userSession) throws IOException {        
        log.debug("TransportInteractionWebSocket: onOpen");      
        this.session = userSession;                   
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param statusCode code of error in case of problems
     * @param reason the reason for connection close
     */
    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        log.debug("TransportInteractionWebSocket: onClose, code: " + statusCode + ", reason: " + reason );
        this.session = null;                
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnWebSocketMessage
    public void onMessage(String message) {
        log.debug("onMessage: "+ message);
        
        try {            
            org.codehaus.jackson.JsonNode parent= new ObjectMapper().readTree(message); 
            if (parent.has("type")) {                
                
                String type =  parent.get("type").asText();
                log.debug("type: " + type );
                
                if  (type.equals("GETSTATUSREPLY")) {
                    log.debug("GetStatusReply");                        
                    TransportStatusReply transportStatusReply = processData(TransportStatusReply.class, message, false);
                        
                    if (transportStatusReply.status.equals("NOT_LAUNCHED")) {
                        log.debug("received status: not launched, configuring internals");
                        secureTransportStatus = SecureTransportStatus.NOT_LAUNCHED;
                    } else if (transportStatusReply.status.equals("CONNECTED")) {
                        log.debug("received status: CONNECTED, saving parameters");
                        // {"type":"GETSTATUSREPLY","status":"CONNECTED","remoteip":"51.15.249.23","remoteport":"25000","tunaddr":"10.75.110.216","tunnetmask":"255.255.255.0","id":"230"}
                        this.remoteip= transportStatusReply.remoteip;
                        this.remoteport = transportStatusReply.remoteport;
                        this.tunaddr = transportStatusReply.tunaddr;
                        this.tunnetmask = transportStatusReply.tunnetmask;                        
                        log.debug("connected to: {} : {} via : {} / {}", remoteip, remoteport, tunaddr, tunnetmask);
                        secureTransportStatus = SecureTransportStatus.CONNECTED;                        
                    } else if (transportStatusReply.status.equals("INITIAL")) {
                        // seems that we are dealing with service that have never been started.. Starting it up
                        secureTransportStatus = SecureTransportStatus.NOT_LAUNCHED;
                    }
                    
                } else if (type.equals("STARTREPLY")) {
                    log.debug("StartReply, setting PENDING and waiting for connect event");
                    secureTransportStatus = SecureTransportStatus.PENDING;
                }                
                
            } else if (parent.has("event")) {
                                
                String eventSpec =  parent.get("event").asText();
                if (eventSpec.equals("CONNECT") ) {
                    // handling json here 
                    TransportEventDescriptor transportEventDescriptor = processData(TransportEventDescriptor.class, message, false);
                    this.remoteip= transportEventDescriptor.remoteip;
                    this.remoteport = transportEventDescriptor.remoteport;
                    this.tunaddr = transportEventDescriptor.tunaddr;
                    this.tunnetmask = transportEventDescriptor.tunnetmask;                   
                    log.debug("connected to: {} : {} via : {} / {}", remoteip, remoteport, tunaddr, tunnetmask);
                    secureTransportStatus = SecureTransportStatus.CONNECTED;                    
                } else if (eventSpec.equals("DISCONNECT")) {
                    TransportEventDescriptor transportEventDescriptor = processData(TransportEventDescriptor.class, message, false);
                    cleanupComParams();                    
                    log.debug("disconnected from: {} : {} via : {} / {}", remoteip, remoteport, tunaddr, tunnetmask);
                    secureTransportStatus = SecureTransportStatus.DISCONNECTED;                    
                }
            }
        } catch (IOException ex) {
            log.error("incoming message processing error: " + ex.toString());
        }

   
    }
    
    /**
     * Send a message.
     *
     * @param message
     */
    public void sendMessage(String message) {        
        try {
            Future<Void> fut;            
            fut = session.getRemote().sendStringByFuture(message);
            fut.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            log.error ("sendMessage InterruptedException: " + ex.getMessage().toString() );
        } catch (ExecutionException ex) {
            log.error ("sendMessage ExecutionException: " + ex.getMessage().toString() );
        } catch (TimeoutException ex) {
            log.error ("sendMessage TimeoutException: " + ex.getMessage().toString() );
        }
    }
    
    /**
     * Checking out whether the connection is open
     */

    public boolean isOpen() {  
        return this.session != null;        
    }
    
    /**
     * Resetting communication parameters
     */
    
    private void cleanupComParams() {
        this.remoteip= "";
        this.remoteport = -1;
        this.tunaddr = "";
        this.tunnetmask = "";
    }


    /**
     * Inquiring status of connection from transport service
     */

    private void getSecureTransportStatus( ) {                
        log.debug("TransportInteractionWebSocket: getSecureTransportStatus");
        TransportStopRequest stopRequest = new TransportStopRequest();
        stopRequest.type = "GETSTATUSREQUEST";
        Random rand = new Random();
        stopRequest.id = rand.nextInt(255);          
        ObjectMapper mapper = new ObjectMapper();                
        try {
            String stopRequestString = mapper.writeValueAsString(stopRequest);            
            log.debug("getting status: " + stopRequestString);            
            sendMessage(stopRequestString);            
        } catch (JsonProcessingException ex) {
            log.error("TransportInteractionWebSocket: Error while creating Getting Status request");                   
        } catch (IOException ex) {
            log.error("getting status error: " + ex.getMessage().toString());
        }
        
    }
    
    
    /**
     * Starting transport
     */
    
    public void startSecureTransport() {
        // creating start package
        TransportStartRequest startRequest = new TransportStartRequest();
        startRequest.type = "STARTREQUEST";
        startRequest.serversjson = this.serversJSON;
        startRequest.uniqueenable = true; 
        startRequest.uniqueport = this.uniquePort;
        startRequest.shuffle = this.shuffle;
        startRequest.logpath = this.logPath;                        
        Random rand = new Random();
        startRequest.id = rand.nextInt(255);                
        ObjectMapper mapper = new ObjectMapper();
                
        try {
            String startRequestString = mapper.writeValueAsString(startRequest); 
            sendMessage(startRequestString);
        } catch (JsonProcessingException ex) {
            log.error("TransportInteractionWebSocket: JSON Error while creating STARTREQUEST");                                       
        } catch (IOException ex) {
            log.error("TransportInteractionWebSocket: IO Error while creating STARTREQUEST");
        }
        
    }
    
    /**
     * Stopping transport
     */
    public void stopSecureTransport() {
        TransportStopRequest stopRequest = new TransportStopRequest();
        stopRequest.type = "STOPREQUEST";
        Random rand = new Random();
        stopRequest.id = rand.nextInt(255);  
        ObjectMapper mapper = new ObjectMapper();
                
        try {
            String stopRequestString = mapper.writeValueAsString(stopRequest); 
            sendMessage(stopRequestString);                
        } catch (JsonProcessingException ex) {                    
            log.error("TransportInteractionWebSocket: JSON Error while creating STOPREQUEST");                                                       
        } catch (IOException ex) {                    
            log.error("TransportInteractionWebSocket: IO Error while creating STOPREQUEST");                
        }        
        cleanupComParams();
        secureTransportStatus = SecureTransportStatus.DISCONNECTED;
    }
    
    
    /**
     * State machine - tick engine
     */
    void tick() {
        
        if (isOpen()) {
            
            switch (secureTransportStatus) {
                case INITIAL: {
                    log.debug("Initial state, need to figure out the status of connection");                    
                    getSecureTransportStatus();
                    break;
                }
                
                case NOT_LAUNCHED: { 
                    log.debug("not launched, startup required");
                    // launching here
                    startSecureTransport();                    
                    break;
                }                
            }            
        }
    }
    
    
    
    /**
     * Starting transport
     * @return status of connection as string 
     */
    String getRemoteConnectionStatus() {
        return secureTransportStatus.toString();
    }

    
    /**
     * Template for JSON processing
     * @param serviceClass - specification of the class to be handled
     * @param inputData - JSON as string
     * @param screening - whether JSON is screened or not
     * @return T - instance of service class, provided as the 1-st argument of input 
     */    
    private static <T> T processData(Class<T> serviceClass, String inputData, boolean screening) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        if (screening) mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        T res = null;
        try {
            res = mapper.readValue(inputData, serviceClass);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }    
}
