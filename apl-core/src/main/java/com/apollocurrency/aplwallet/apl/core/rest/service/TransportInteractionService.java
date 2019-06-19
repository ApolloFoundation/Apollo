/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest.service;

import com.apollocurrency.aplwallet.api.response.TransportStatusResponse;
import javax.inject.Singleton;

@Singleton
public interface TransportInteractionService {
    
    /**
     * Get transport status     
     * @return TransportStatusResponse <i>null</i>
     */
       
    public TransportStatusResponse getTransportStatusResponse();  
    
    /**
     * Starting up with secure transport     
     */

    public void startSecureTransport();
    
    /**
     * Stopping secure transport     
     */

    public void stopSecureTransport();
        
    /** 
     * Start transport interaction service
     */
     
    public void start();
   
    /** 
     * Start transport interaction service
     */
     
    public void stop();
    
    
}
