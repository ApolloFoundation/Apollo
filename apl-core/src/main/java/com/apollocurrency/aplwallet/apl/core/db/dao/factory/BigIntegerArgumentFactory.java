package com.apollocurrency.aplwallet.apl.core.db.dao.factory;

import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.math.BigInteger;
import java.sql.Types;

public class BigIntegerArgumentFactory extends AbstractArgumentFactory<BigInteger> {
    public BigIntegerArgumentFactory() {
        super(Types.VARCHAR);
    }

    @Override
    protected Argument build(BigInteger value, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setString(position, value.toString());
    }
}
