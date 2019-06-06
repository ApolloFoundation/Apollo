/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import java.net.URI;
import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ClientEndpoint
public class TransportInteractionWebSocket {
        
    Session userSession = null;
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionWebSocket.class);

    public TransportInteractionWebSocket(URI endpointURI) {
        
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            // throw new RuntimeException(e);
            log.debug("Not connected at the moment");            
        }       
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {        
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
    
}
