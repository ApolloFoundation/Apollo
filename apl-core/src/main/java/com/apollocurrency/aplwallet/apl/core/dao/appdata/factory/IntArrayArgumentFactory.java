package com.apollocurrency.aplwallet.apl.core.dao.appdata.factory;

import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;
import java.util.Arrays;

@Slf4j
public class IntArrayArgumentFactory extends AbstractArgumentFactory<int[]> {
    public IntArrayArgumentFactory() {
        super(Types.LONGVARCHAR);
    }

    @Override
    protected Argument build(int[] value, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setString(position, Arrays.toString(value));
    }
}
