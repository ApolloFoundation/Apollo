package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class OpArgumentFactory extends AbstractArgumentFactory<DexTransaction.Op> {

    public OpArgumentFactory() {
        super(Types.TINYINT);
    }

    @Override
    public Argument build(final DexTransaction.Op op, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setByte(position, op.getCode());
    }
}
