package com.apollocurrency.aplwallet.apl.core.db.dao.factory;

import com.apollocurrency.aplwallet.apl.exchange.model.OfferStatus;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.sql.Types;

public class OfferStatusFactory  extends AbstractArgumentFactory<OfferStatus> {


    public OfferStatusFactory() {
        super(Types.INTEGER);
    }

    @Override
    public Argument build(final OfferStatus offerStatus, ConfigRegistry config) {
        return (position, statement, ctx) -> statement.setInt(position, offerStatus.ordinal());
    }
}
