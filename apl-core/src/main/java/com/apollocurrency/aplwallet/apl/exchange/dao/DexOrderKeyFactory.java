/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.exchange.dao;

import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
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
