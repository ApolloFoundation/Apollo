/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.config.DaoConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.cdi.transaction.JdbiHandleFactory;
import com.apollocurrency.aplwallet.apl.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.model.DexTradeEntry;
import com.apollocurrency.aplwallet.apl.extension.DbExtension;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/**
 *
 * @author Serhiy Lymar
 */

@EnableWeld
public class DexTradeDaoTest {
    @RegisterExtension
    static DbExtension extension = new DbExtension();
    
    @WeldSetup
    public WeldInitiator weld = WeldInitiator.from(
            DaoConfig.class,
            DexTradeDao.class
            )
            .addBeans(MockBean.of(extension.getDatabaseManager(), DatabaseManager.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbi(), Jdbi.class))
            .addBeans(MockBean.of(extension.getDatabaseManager().getJdbiHandleFactory(), JdbiHandleFactory.class))
            .build();
    
    
    @Inject
    private DexTradeDao dao;

    @Test
    void testGetAllExisting() {
        Integer startTime = 53409447; // hardcoded in 'data.sql' - included value
        Integer finishTime = 53409464; // hardcoded in 'data.sql' - excluded value
        List<DexTradeEntry>  result = dao.getDexEntriesForInterval(startTime, finishTime, (byte)1, 0, 1000);
        assertNotNull(result);
        assertEquals(6, result.size());
    }

    @Test
    void testInsert() {
        //clean all data inserted from 'data.sql'
        dao.hardDeleteAllDexTrade();

        Integer currentTimeFake = 1234567890;
        List<DexTradeEntry> storedEntries =  new ArrayList<>();        
        Integer discr = 10000; 
        Integer iters = 5;
        Integer height = 45123; 
        Random random = new Random();
        
        for ( Integer i = currentTimeFake; i<= currentTimeFake + (iters * discr); i+= discr ) {
            DexTradeEntry dexTradeEntryWrite = new DexTradeEntry(null, null);
            dexTradeEntryWrite.setSenderOfferID(random.nextLong());        
            dexTradeEntryWrite.setReceiverOfferID(random.nextLong());
            dexTradeEntryWrite.setSenderOfferType((byte)0);        
            dexTradeEntryWrite.setSenderOfferCurrency((byte)0);        
            dexTradeEntryWrite.setSenderOfferAmount(random.nextLong());
            dexTradeEntryWrite.setPairCurrency((byte)1);
            dexTradeEntryWrite.setPairRate(BigDecimal.valueOf(random.nextLong()));           
            Long randomTransactionID = random.nextLong();
            dexTradeEntryWrite.setTransactionID(randomTransactionID);
            dexTradeEntryWrite.setFinishTime(i);            
            dexTradeEntryWrite.setHeight(height);
            height++;                        
            dao.saveDexTradeEntry(dexTradeEntryWrite);    
            storedEntries.add(dexTradeEntryWrite);
        }       
        
        // reading... 
        List<DexTradeEntry> extractedTradeEntries = dao.getDexEntriesForInterval(currentTimeFake, Integer.MAX_VALUE, (byte)1, 0, 1000);
        assertNotNull(extractedTradeEntries);
                        
        assert(extractedTradeEntries.size() == (iters + 1));
        
        for (Integer i=0; i<extractedTradeEntries.size(); i++) {           
            DexTradeEntry current = extractedTradeEntries.get(i);
            DexTradeEntry stored  = storedEntries.get(i);
            // comparing current 
            assert ( current.getSenderOfferID() == stored.getSenderOfferID() );
            assert ( current.getReceiverOfferID() == stored.getReceiverOfferID() );
            assert ( current.getSenderOfferType() == stored.getSenderOfferType());
            assert ( current.getSenderOfferCurrency() == stored.getSenderOfferCurrency());
            assert ( current.getSenderOfferAmount() == stored.getSenderOfferAmount() );
            assert( current.getPairCurrency() == stored.getPairCurrency());
            BigDecimal currentPairRate = new BigDecimal( EthUtil.ethToGwei(current.getPairRate()));            
            assert( currentPairRate.equals(stored.getPairRate()) );         
            assert( current.getTransactionID() == stored.getTransactionID());
            assert( current.getFinishTime() == stored.getFinishTime() );
            assert( current.getHeight() == stored.getHeight());
        }
    }
    
    @Test
    void testFillStructurized() {
        //clean all data inserted from 'data.sql'
        dao.hardDeleteAllDexTrade();
        
        
        final long initialTS = 1569888000000L; // GMT: Tuesday, 1 October 2019 г., 0:00:00   
        final long endTimestemp =  1574208000000L; // Date and time (GMT): Wednesday, 20 November 2019 г., 0:00:00
        // final long discreteInterval = 60; // sec for histominute
        final long testUpdateInterval = 10; // sec between closed orders

        long currentTimeFake = initialTS;
        List<DexTradeEntry> storedEntries =  new ArrayList<>();        
        long  discr = testUpdateInterval * 1000; // in us
        Integer iters = 5;
        Integer height = 45123; 
        Random random = new Random();
        
        
        long currentTime = System.currentTimeMillis();
        
        
        for ( Long i = currentTimeFake; i<= currentTime; i+= discr ) {
            DexTradeEntry dexTradeEntryWrite = new DexTradeEntry(null, null);
            dexTradeEntryWrite.setSenderOfferID(random.nextLong());        
            dexTradeEntryWrite.setReceiverOfferID(random.nextLong());
            dexTradeEntryWrite.setSenderOfferType((byte)0);        
            dexTradeEntryWrite.setSenderOfferCurrency((byte)0);        
            dexTradeEntryWrite.setSenderOfferAmount(random.nextLong());
            dexTradeEntryWrite.setPairCurrency((byte)1);
            dexTradeEntryWrite.setPairRate(BigDecimal.valueOf(random.nextLong()));           
            Long randomTransactionID = random.nextLong();
            dexTradeEntryWrite.setTransactionID(randomTransactionID);
            dexTradeEntryWrite.setFinishTime(i);            
            dexTradeEntryWrite.setHeight(height);
            height++;                        
            dao.saveDexTradeEntry(dexTradeEntryWrite);    
            // storedEntries.add(dexTradeEntryWrite);
        }       
/*        
        // reading... 
        List<DexTradeEntry> extractedTradeEntries = dao.getDexEntriesForInterval(currentTimeFake, Integer.MAX_VALUE, (byte)1, 0, 1000);
        assertNotNull(extractedTradeEntries);
                        
        assert(extractedTradeEntries.size() == (iters + 1));
        
        for (Integer i=0; i<extractedTradeEntries.size(); i++) {           
            DexTradeEntry current = extractedTradeEntries.get(i);
            DexTradeEntry stored  = storedEntries.get(i);
            // comparing current 
            assert ( current.getSenderOfferID() == stored.getSenderOfferID() );
            assert ( current.getReceiverOfferID() == stored.getReceiverOfferID() );
            assert ( current.getSenderOfferType() == stored.getSenderOfferType());
            assert ( current.getSenderOfferCurrency() == stored.getSenderOfferCurrency());
            assert ( current.getSenderOfferAmount() == stored.getSenderOfferAmount() );
            assert( current.getPairCurrency() == stored.getPairCurrency());
            BigDecimal currentPairRate = new BigDecimal( EthUtil.ethToGwei(current.getPairRate()));            
            assert( currentPairRate.equals(stored.getPairRate()) );         
            assert( current.getTransactionID() == stored.getTransactionID());
            assert( current.getFinishTime().equals(stored.getFinishTime()) );
            assert( current.getHeight() == stored.getHeight());
        }
*/
    }
    
    
}
