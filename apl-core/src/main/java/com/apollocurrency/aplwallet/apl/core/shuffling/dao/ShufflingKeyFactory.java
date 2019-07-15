/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;

import javax.inject.Singleton;

@Singleton
public class ShufflingKeyFactory extends LongKeyFactory<Shuffling> {

    public ShufflingKeyFactory() {
        super("id");
    }

    @Override
    public DbKey newKey(Shuffling shuffling) {
        if (shuffling.getDbKey() == null) {
            shuffling.setDbKey(new LongKey(shuffling.getId()));
        }
        return shuffling.getDbKey();
    }

}
