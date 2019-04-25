/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db.dao.factory;

import com.apollocurrency.aplwallet.apl.exchange.model.DexCurrencies;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class DexCurrenciesFactory  extends AbstractArgumentFactory<DexCurrencies> {

    public DexCurrenciesFactory() {
        super(Types.INTEGER);
    }

    @Override
    public Argument build(final DexCurrencies dexCurrencies, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setInt(position, dexCurrencies.ordinal());
    }
}