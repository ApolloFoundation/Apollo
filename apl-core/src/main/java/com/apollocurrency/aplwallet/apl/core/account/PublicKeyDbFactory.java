/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.account;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;

/**
 *
 * @author al
 */
class PublicKeyDbFactory extends LongKeyFactory<PublicKey> {
    
    public PublicKeyDbFactory(String idColumn) {
        super(idColumn);
    }

    @Override
    public DbKey newKey(PublicKey publicKey) {
        return publicKey.dbKey;
    }

    @Override
    public PublicKey newEntity(DbKey dbKey) {
        return new PublicKey(((DbKey.LongKey) dbKey).getId(), null);
    }
    
}
