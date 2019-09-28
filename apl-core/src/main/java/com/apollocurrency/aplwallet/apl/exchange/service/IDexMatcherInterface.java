/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.service;


/**
 *
 * @author Serhiy Lymar
 */
public interface IDexMatcherInterface {
     /** 
     * Start transport interaction service
     */
     
    public void initialize();
   
    /** 
     * Stop transport interaction service
     */
     
    public void deinitialize();
    
}
