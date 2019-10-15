package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.exchange.model.DexOrder;

import javax.inject.Singleton;

@Singleton
public class DexOrderKeyFactory extends LongKeyFactory<DexOrder> {
    public DexOrderKeyFactory() {
        super("id");
    }

    @Override
    public DbKey newKey(DexOrder offer) {
        return new LongKey(offer.getId());
    }
}
