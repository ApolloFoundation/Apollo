/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.dao.appdata.factory;

import com.apollocurrency.aplwallet.apl.dex.core.model.DexCurrency;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class DexCurrenciesFactory extends AbstractArgumentFactory<DexCurrency> {

    public DexCurrenciesFactory() {
        super(Types.INTEGER);
    }

    @Override
    public Argument build(final DexCurrency dexCurrency, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setInt(position, dexCurrency.ordinal());
    }
}