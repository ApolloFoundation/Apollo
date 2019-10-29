/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.core.rest.service;

import javax.inject.Singleton;

/**
 *
 * @author nemez
 */

@Singleton
public interface TradingViewService {
    
    
    /** 
     * Start transport interaction service
     */
     
    public void start();
   
    /** 
     * Start transport interaction service
     */
     
    public void stop();
    
}
