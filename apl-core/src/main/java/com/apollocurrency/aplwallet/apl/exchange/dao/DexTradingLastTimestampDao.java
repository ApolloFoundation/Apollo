/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exchange.dao;


import com.apollocurrency.aplwallet.apl.core.db.cdi.Transactional;
import com.apollocurrency.aplwallet.apl.core.db.dao.mapper.BlockIndexRowMapper;
import com.apollocurrency.aplwallet.apl.core.db.dao.model.BlockIndex;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;



/**
 *
 * @author Serhiy Lymar
 */
public interface DexTradingLastTimestampDao {
 // SELECT LAST_TIMESTAMP FROM "PUBLIC".TRADING_TRACK LIMIT 100;
    
    @Transactional(readOnly = true)
    @SqlQuery("SELECT LAST_TIMESTAMP FROM TRADING_TRACK")
    // @RegisterRowMapper(long)
    long getLast();
    
    @Transactional
    @SqlUpdate("INSERT INTO LAST_TIMESTAMP( LAST_TIMESTAMP ) " +
            "VALUES ( :lastTimestamp)")
    int saveBlockIndex(@BindBean long lastTimestamp);
    
    @Transactional
    @SqlUpdate("UPDATE TRADING_TRACK SET block_height =:blockHeight where block_id =:blockId")
            
    // UPDATE TRADING_TRACK SET LAST_TIMESTAMP= :lastTimestamp WHERE LAST_TIMESTAMP =(SELECT LAST_TIMESTAMP FROM TRADING_TRACK LIMIT 1) 
    int updateBlockIndex(@BindBean long lastTimestamp);
    
    @Transactional
    @SqlUpdate("DELETE FROM TRADING_TRACK")
    int hardDeleteLast();


}
