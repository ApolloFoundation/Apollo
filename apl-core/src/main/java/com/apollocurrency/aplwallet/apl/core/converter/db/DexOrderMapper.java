/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.converter.db;

import com.apollocurrency.aplwallet.apl.core.dao.state.mapper.VersionedDerivedEntityMapper;
import com.apollocurrency.aplwallet.apl.core.model.dex.DexOrder;
import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import com.apollocurrency.aplwallet.apl.dex.core.model.OrderType;
import com.apollocurrency.aplwallet.apl.dex.eth.utils.EthUtil;
import com.apollocurrency.aplwallet.apl.exchange.dao.DexOrderKeyFactory;
import org.jdbi.v3.core.statement.StatementContext;

import javax.inject.Singleton;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class DexOrderMapper extends VersionedDerivedEntityMapper<DexOrder> {

    public DexOrderMapper() {
        super(new DexOrderKeyFactory());
    }

    @Override
    public DexOrder doMap(ResultSet rs, StatementContext ctx) throws SQLException {
        DexOrder dexOrder = new DexOrder();

        dexOrder.setId(rs.getLong("id"));
        dexOrder.setAccountId(rs.getLong("account_id"));
        dexOrder.setType(OrderType.getType(rs.getInt("type")));
        dexOrder.setOrderCurrency(DexCurrency.getType(rs.getInt("offer_currency")));
        dexOrder.setOrderAmount(rs.getLong("offer_amount"));
        dexOrder.setPairCurrency(DexCurrency.getType(rs.getInt("pair_currency")));
        //TODO change type in the db (migrate existing data).
        dexOrder.setPairRate(EthUtil.gweiToEth(rs.getLong("pair_rate")));
        dexOrder.setFinishTime(rs.getInt("finish_time"));
        dexOrder.setStatus(OrderStatus.getType(rs.getInt("status")));
        dexOrder.setFromAddress(rs.getString("from_address"));
        dexOrder.setToAddress(rs.getString("to_address"));

        return dexOrder;
    }
}