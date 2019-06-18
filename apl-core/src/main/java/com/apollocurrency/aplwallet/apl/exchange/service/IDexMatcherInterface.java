/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.apollocurrency.aplwallet.apl.exchange.service;
import javax.inject.Singleton;


/**
 *
 * @author Serhiy Lymar
 */

@Singleton
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
