package com.apollocurrency.aplwallet.apl.core.dao.appdata.factory;

import com.apollocurrency.aplwallet.apl.dex.core.model.OrderStatus;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class OrderStatusFactory extends AbstractArgumentFactory<OrderStatus> {


    public OrderStatusFactory() {
        super(Types.INTEGER);
    }

    @Override
    public Argument build(final OrderStatus orderStatus, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setInt(position, orderStatus.ordinal());
    }
}
