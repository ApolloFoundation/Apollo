package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.dex.core.model.DexOperation;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class DexOperationStageArgumentFactory extends AbstractArgumentFactory<DexOperation.Stage> {

    public DexOperationStageArgumentFactory() {
        super(Types.TINYINT);
    }

    @Override
    public Argument build(final DexOperation.Stage op, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setByte(position, op.getCode());
    }
}
