package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;
import com.apollocurrency.aplwallet.apl.core.rest.endpoint.TransportInteractionController;
import java.net.URI;
import java.net.URISyntaxException;
import javax.inject.Singleton;
import org.eclipse.jetty.util.log.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransportInteractionServiceImpl implements TransportInteractionService {
    
    
    private static final Logger log = LoggerFactory.getLogger(TransportInteractionServiceImpl.class);
    private static TransportInteractionServiceImpl instance = null; 
    
    TransportInteractionServiceImpl() {
        
        Log.getLog().info("Initializing TransportInteractionServiceImpl");
        System.out.println("Initializing TransportInteractionServiceImpl");
        
    }
    
    public static TransportInteractionServiceImpl getInstance() 
    { 
        if ( instance == null) 
            instance = new TransportInteractionServiceImpl(); 
  
        return instance; 
    }

    @Override
    public TransportStatusResponse getTransportStatusResponse() {
        TransportStatusResponse transportStatusResponse =  new TransportStatusResponse();
        
        Log.getLog().info("getTransportStatusResponse");
        System.out.println("getTransportStatusResponse");
        
        return transportStatusResponse;
    }
    
    public void ignite() {
        
        System.out.println("Ingition point: "); 
         
         try {
            // open websocket
            
            TransportInteractionWebSocket transportInteractionWebSocket = new TransportInteractionWebSocket(new URI("ws://127.0.0.1:8888/"));

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