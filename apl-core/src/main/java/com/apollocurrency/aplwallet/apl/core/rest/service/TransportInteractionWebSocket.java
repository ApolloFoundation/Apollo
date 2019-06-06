/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.transport.TransportGenericPacket;
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
        
    Session userSession = null;
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionWebSocket.class);
    
    private SecureTransportStatus secureTransportStatus;
    
    // status of the transport connection
    enum SecureTransportStatus {
        INITIAL, NOT_LAUNCHED, PENDING, CONNECTED, DISCONNECTED
    }
    

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
                    }
                    
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
            java.util.logging.Logger.getLogger(TransportInteractionWebSocket.class.getName()).log(Level.SEVERE, null, ex);
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
