package com.apollocurrency.aplwallet.apl.core.dao.appdata.factory;

import com.apollocurrency.aplwallet.apl.core.shard.helper.jdbc.SimpleResultSet;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class LongArrayArgumentFactory extends AbstractArgumentFactory<Long[]> {
    public LongArrayArgumentFactory() {
        super(Types.ARRAY);
    }

    @Override
    protected Argument build(Long[] value, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setArray(position, new SimpleResultSet.SimpleArray(value));
    }
}
