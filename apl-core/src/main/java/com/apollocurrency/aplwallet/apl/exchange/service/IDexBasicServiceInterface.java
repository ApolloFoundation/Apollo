/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;

/**
 *
 * @author Serhiy Lymar
 */

public interface IDexBasicServiceInterface {
     /** 
     * Start transport interaction service
     */
     void initialize();
   
    /** 
     * Stop transport interaction service
     */
    void deinitialize();
    
}
