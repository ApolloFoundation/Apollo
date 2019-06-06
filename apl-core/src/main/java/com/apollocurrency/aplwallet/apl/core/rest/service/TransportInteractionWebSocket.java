/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.transport.TransportEventDescriptor;
import com.apollocurrency.aplwallet.api.transport.TransportGenericPacket;
import com.apollocurrency.aplwallet.api.transport.TransportStartRequest;
import com.apollocurrency.aplwallet.api.transport.TransportStatusReply;
import com.apollocurrency.aplwallet.api.transport.TransportStopRequest;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

import java.net.URI;
import java.util.Random;
import java.util.logging.Level;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ClientEndpoint
public class TransportInteractionWebSocket {
    
    
    private String serversJSON = "./servers.json";
    private boolean shuffle = true, uniqueEnable = false; 
    private int uniquePort = -1; 
    private String logPath = "/var/log";   
        
    Session userSession = null;
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionWebSocket.class);
    
    private SecureTransportStatus secureTransportStatus;
    
    // status of the transport connection
    enum SecureTransportStatus {
        INITIAL, NOT_LAUNCHED, PENDING, CONNECTED, DISCONNECTED
    }
    
    
    public String remoteip;    
    public int remoteport;    
    public String tunaddr;        
    public String tunnetmask;  
    
    public TransportInteractionWebSocket(URI endpointURI) {
        
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            // throw new RuntimeException(e);
            log.debug("Not connected at the moment");            
        } 
        
        secureTransportStatus = SecureTransportStatus.INITIAL;
        
        
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) throws IOException {        
        log.debug("TransportInteractionWebSocket: onOpen");      
        this.userSession = userSession;                   
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {        
        log.debug("TransportInteractionWebSocket: onClose, reason: " + reason.getReasonPhrase() );
        this.userSession = null;                
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        log.debug("onMessage: "+ message);
        
        try {
            // final ObjectNode node = new ObjectMapper().readValue(json, ObjectNode.class);
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
                        log.debug("connected to: " + remoteip + ":" + remoteport + " via : " + tunaddr + "/" + tunnetmask);
                        secureTransportStatus = SecureTransportStatus.CONNECTED;                        
                    }
                    
                } else if (type.equals("STARTREPLY")) {
                    log.debug("StartReply, setting PENDING and waiting for connect event");
                    secureTransportStatus = SecureTransportStatus.PENDING;
                }                
                
            } else if (parent.has("event")) {
                
                // {"event":"CONNECT","remoteip":"51.15.249.23","remoteport":"25000","tunaddr":"10.75.110.216","tunnetmask":"255.255.255.0"}
                
                String eventSpec =  parent.get("event").asText();
                if (eventSpec.equals("CONNECT") ) {
                    // handling json here 
                    TransportEventDescriptor transportEventDescriptor = processData(TransportEventDescriptor.class, message, false);
                    this.remoteip= transportEventDescriptor.remoteip;
                    this.remoteport = transportEventDescriptor.remoteport;
                    this.tunaddr = transportEventDescriptor.tunaddr;
                    this.tunnetmask = transportEventDescriptor.tunnetmask;
                    log.debug("connected to: " + remoteip + ":" + remoteport + " via : " + tunaddr + "/" + tunnetmask);
                    secureTransportStatus = SecureTransportStatus.CONNECTED;                    
                } else if (eventSpec.equals("DISCONNECT")) {
                    TransportEventDescriptor transportEventDescriptor = processData(TransportEventDescriptor.class, message, false);
                    this.remoteip= "";
                    this.remoteport = -1;
                    this.tunaddr = "";
                    this.tunnetmask = "";
                    log.debug("disconnected from: " + remoteip + ":" + remoteport + " via : " + tunaddr + "/" + tunnetmask + ", reconnecting");
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
        this.userSession.getAsyncRemote().sendText(message);
    }
    
    /**
     * Checking out whether the connection is open
     *
     * 
     */

    public boolean isOpen() {  
        return this.userSession != null;
        
    }


    private void getSecureTransportStatus( ) {
                
        log.debug("TransportInteractionWebSocket: getSecureTransportStatus");
        TransportStopRequest stopRequest = new TransportStopRequest();
        stopRequest.type = "GETSTATUSREQUEST";
        Random rand = new Random();
        stopRequest.id = rand.nextInt(255);  
        
        ObjectMapper mapper = new ObjectMapper();
                
        try {
            String stopRequestString = mapper.writeValueAsString(stopRequest);
            // sendMessage(stopRequestString);            
            log.debug("getting status: " + stopRequestString);
            
            sendMessage(stopRequestString);
            
        } catch (JsonProcessingException ex) {
            log.error("TransportInteractionWebSocket: Error while creating Getting Status request");                   
        } catch (IOException ex) {
            log.error("getting status error: " + ex.getMessage().toString());
        }
        
    }
    
    private void startSecureTransport() {
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
    

    
    void tick() {
        log.debug("tick");
        if (isOpen()) {
            log.debug("ws is connected, handling the situation according to the routine, status: " + secureTransportStatus );
            
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
            
        } else {
            log.debug("closed at the moment");
        }        
    }


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
