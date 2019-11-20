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
    
    @Transactional(readOnly = true)
    @SqlQuery("SELECT LAST_TIMESTAMP FROM TRADING_TRACK")
    long getLastTimestamp();
    
    @Transactional
    @SqlUpdate("INSERT INTO TRADING_TRACK VALUES ( :lastTimestamp )")
    int saveLastTimestamp(@BindBean long lastTimestamp);
    
    @Transactional
    @SqlUpdate("UPDATE TRADING_TRACK SET LAST_TIMESTAMP =:lastTimestamp")
    int updateLastTimestamp(@BindBean long lastTimestamp);
    
    @Transactional
    @SqlUpdate("DELETE FROM TRADING_TRACK")
    int hardDeleteLastTimestamp();

}
