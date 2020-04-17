/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.dao.factory;

import com.apollocurrency.aplwallet.apl.core.db.dao.model.ShardState;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class ShardStateFactory extends AbstractArgumentFactory<ShardState> {


    public ShardStateFactory() {
        super(Types.BIGINT);
    }

    @Override
    public Argument build(final ShardState shardState, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setLong(position, shardState.getValue()); // internal value (not ordinary) put into db
    }
}
