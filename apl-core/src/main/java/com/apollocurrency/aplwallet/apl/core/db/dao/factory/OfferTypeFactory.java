/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.db.dao.factory;

import com.apollocurrency.aplwallet.apl.exchange.model.OfferType;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class OfferTypeFactory extends AbstractArgumentFactory<OfferType> {

    public OfferTypeFactory() {
        super(Types.INTEGER);
    }

    @Override
    public Argument build(final OfferType offerType, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setInt(position, offerType.ordinal());
    }
}