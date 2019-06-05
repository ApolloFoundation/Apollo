package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.TransportInteractionController;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransportInteractionServiceImpl implements TransportInteractionService {
    
    
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionServiceImpl.class);
   
    private String wsUrl; 
    
    @Inject
    TransportInteractionServiceImpl( PropertiesHolder prop ) {
        
        log.debug("Initializing TransportInteractionServiceImpl");   
        wsUrl = prop.getStringProperty("apl.securetransporturl","ws://localhost:8888/");
        
        
        
    }
    

    @Override
    public TransportStatusResponse getTransportStatusResponse() {
        TransportStatusResponse transportStatusResponse =  new TransportStatusResponse();
        
        log.debug("getTransportStatusResponse");
        
        return transportStatusResponse;
    }
    
    @Override
    public void start() {
        
        log.debug("Ingition point: "); 
         
         try {
            // open websocket
            
            TransportInteractionWebSocket transportInteractionWebSocket = new TransportInteractionWebSocket(new URI(wsUrl));

//            // add listener
//            clientEndPoint.addMessageHandler(new TransportInteractionWebSocket.MessageHandler() {
//                public void handleMessage(String message) {
//                    System.out.println(message);
//                }
//            });
//
//            // send message to websocket
//            
//            // clientEndPoint.sendMessage("{'event':'addChannel','channel':'ok_btccny_ticker'}");
//            
//            clientEndPoint.sendMessage("{\"type\":\"STOPREQUEST\",\"id\":241}");
//            
//            // stopping
//
//            // wait 5 seconds for messages from websocket
//
//                
//            Thread.sleep(5000);
//            clientEndPoint.sendMessage( "{  \"type\":\"STARTREQUEST\",\"serversjson\":\"./servers.json\",\"uniqueenable\":true,\"shuffle\":true,\"uniqueport\":-1,\"logpath\":\"/Volumes/usersd/Users/nemez/Desktop\",\"id\":46}");
//
//            
//            for(;;) {
//                Thread.sleep(5000);
//                
//            }
            
            // clientEndPoint.sendMessage( "{  \"type\":\"STARTREQUEST\",\"serversjson\":\"./servers.json\",\"uniqueenable\":true,\"shuffle\":true,\"uniqueport\":-1,\"logpath\":\"/Volumes/usersd/Users/nemez/Desktop\",\"id\":46}");
            
            
            

//        } catch (InterruptedException ex) {
//            System.err.println("InterruptedException exception: " + ex.getMessage());
        } catch (URISyntaxException ex) {
            System.err.println("URISyntaxException exception: " + ex.getMessage());
        }
        
    }
    
}