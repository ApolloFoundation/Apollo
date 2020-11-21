package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.mapper.OrderScanMapper;
import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.exchange.model.OrderScan;
import com.apollocurrency.aplwallet.apl.util.cdi.Transactional;
import org.jdbi.v3.sqlobject.config.RegisterRowMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface OrderScanDao {
    @Transactional
    @SqlUpdate("INSERT INTO order_scan (coin, last_db_id) VALUES (:coin, :lastDbId)")
    void add(@BindBean OrderScan orderScan);

    @Transactional
    @SqlUpdate("UPDATE order_scan SET last_db_id = :lastDbId WHERE coin = :coin")
    void update(@BindBean OrderScan orderScan);

    @Transactional(readOnly = true)
    @SqlQuery("SELECT * FROM order_scan WHERE coin = :coin")
    @RegisterRowMapper(OrderScanMapper.class)
    OrderScan get(@Bind("coin") DexCurrency coin);
}
