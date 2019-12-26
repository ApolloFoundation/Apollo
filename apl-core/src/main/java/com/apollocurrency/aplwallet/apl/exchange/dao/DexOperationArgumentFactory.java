package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.exchange.model.DexTransaction;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class DexOperationArgumentFactory extends AbstractArgumentFactory<DexTransaction.DexOperation> {

    public DexOperationArgumentFactory() {
        super(Types.TINYINT);
    }

    @Override
    public Argument build(final DexTransaction.DexOperation dexOperation, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setByte(position, dexOperation.getCode());
    }
}
