/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.appdata.factory;

import com.apollocurrency.aplwallet.apl.exchange.model.OrderType;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class OrderTypeFactory extends AbstractArgumentFactory<OrderType> {

    public OrderTypeFactory() {
        super(Types.INTEGER);
    }

    @Override
    public Argument build(final OrderType orderType, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setInt(position, orderType.ordinal());
    }
}