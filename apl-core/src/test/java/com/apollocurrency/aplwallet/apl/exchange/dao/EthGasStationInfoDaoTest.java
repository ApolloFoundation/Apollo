/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.model.EthGasInfo;
import com.apollocurrency.aplwallet.apl.exchange.service.DexEthService;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Serhiy Lymar
 */

@EnableWeld
public class EthGasStationInfoDaoTest {
    
    private static int nTests = 10;
    
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            DexEthService.class,
            EthGasStationInfoDao.class)
            .build();
    
    @Inject
    private EthGasStationInfoDao ethGasStationInfoDao;   
    
    @Inject DexEthService dexEthService;
    
    
    Logger logger = Logger.getLogger(EthGasStationInfoDaoTest.class.getName());
    
    @Test
    void testQuery() {
        
        logger.log(Level.INFO,"Teting ETH Gas station query: ");

        try {

            for (int i = 0; i<nTests; i++) {

                logger.log(Level.INFO,"test " + (i+1) + " out of " + nTests );
             
                EthGasInfo ethGasInfo = ethGasStationInfoDao.getEthPriceInfo();            
                assert (ethGasInfo != null);
                
                logger.log(Level.INFO, "average : " + ethGasInfo.getAverageSpeedPrice());
                logger.log(Level.INFO,  "fast : " + ethGasInfo.getFastSpeedPrice());
                logger.log(Level.INFO,  "low  : " + ethGasInfo.getSafeLowSpeedPrice());
                
            }
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    void testService() {
        logger.log(Level.INFO,"Teting ETH Gas station service: ");

        for (int i = 0; i<nTests; i++) {
            
            logger.log(Level.INFO,"test " + (i+1) + " out of " + nTests );
            
            EthGasInfo ethGasInfo=null;
            try {
                ethGasInfo = dexEthService.getEthPriceInfo();
            } catch (ExecutionException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            assert (ethGasInfo != null);
            
            logger.log(Level.INFO, "average : " + ethGasInfo.getAverageSpeedPrice());
            logger.log(Level.INFO,  "fast : " + ethGasInfo.getFastSpeedPrice());
            logger.log(Level.INFO,  "low  : " + ethGasInfo.getSafeLowSpeedPrice());
            
        }        
        
    }
    

    
    @Test
    void testChainQuery() {
        
        logger.log(Level.INFO,"Teting ETH Chain Gas station query: ");

        try {

            for (int i = 0; i<nTests; i++) {

                logger.log(Level.INFO,"test " + (i+1) + " out of " + nTests );
             
                EthGasInfo ethGasInfo = ethGasStationInfoDao.getEthChainPriceInfo();            
                assert (ethGasInfo != null);
                
                logger.log(Level.INFO, "average : " + ethGasInfo.getAverageSpeedPrice());
                logger.log(Level.INFO,  "fast : " + ethGasInfo.getFastSpeedPrice());
                logger.log(Level.INFO,  "low  : " + ethGasInfo.getSafeLowSpeedPrice());
                
            }
            
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    
    
    
    
}
