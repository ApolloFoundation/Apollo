/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.dex.eth.dao.EthGasStationInfoDao;
import com.apollocurrency.aplwallet.apl.dex.eth.service.DexEthService;
import com.apollocurrency.aplwallet.apl.dex.exchange.model.EthGasInfo;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Serhiy Lymar
 */

@Disabled
@EnableWeld
public class EthGasStationInfoDaoTest {

    private static int nTests = 10;

    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
        DexEthService.class,
        EthGasStationInfoDao.class)
        .build();
    @Inject
    DexEthService dexEthService;
    Logger logger = Logger.getLogger(EthGasStationInfoDaoTest.class.getName());
    @Inject
    private EthGasStationInfoDao ethGasStationInfoDao;

    @Test
    void testQuery() {

        logger.log(Level.INFO, "Teting ETH Gas station query: ");

        try {

            for (int i = 0; i < nTests; i++) {

                logger.log(Level.INFO, "test " + (i + 1) + " out of " + nTests);

                EthGasInfo ethGasInfo = ethGasStationInfoDao.getEthPriceInfo();
                assert (ethGasInfo != null);

                logger.log(Level.INFO, "average : " + ethGasInfo.getAverageSpeedPrice());
                logger.log(Level.INFO, "fast : " + ethGasInfo.getFastSpeedPrice());
                logger.log(Level.INFO, "low  : " + ethGasInfo.getSafeLowSpeedPrice());

            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Test
    void testService() {
        logger.log(Level.INFO, "Teting ETH Gas station service: ");

        for (int i = 0; i < nTests; i++) {

            logger.log(Level.INFO, "test " + (i + 1) + " out of " + nTests);

            EthGasInfo ethGasInfo = null;
            try {
                ethGasInfo = dexEthService.getEthPriceInfo();
            } catch (ExecutionException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
            assert (ethGasInfo != null);

            logger.log(Level.INFO, "average : " + ethGasInfo.getAverageSpeedPrice());
            logger.log(Level.INFO, "fast : " + ethGasInfo.getFastSpeedPrice());
            logger.log(Level.INFO, "low  : " + ethGasInfo.getSafeLowSpeedPrice());

        }

    }


    @Test
    void testChainQuery() {

        logger.log(Level.INFO, "Teting ETH Chain Gas station query: ");

        try {

            for (int i = 0; i < nTests; i++) {

                logger.log(Level.INFO, "test " + (i + 1) + " out of " + nTests);

                EthGasInfo ethGasInfo = ethGasStationInfoDao.getEthChainPriceInfo();
                assert (ethGasInfo != null);

                logger.log(Level.INFO, "average : " + ethGasInfo.getAverageSpeedPrice());
                logger.log(Level.INFO, "fast : " + ethGasInfo.getFastSpeedPrice());
                logger.log(Level.INFO, "low  : " + ethGasInfo.getSafeLowSpeedPrice());

            }

        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }


}
