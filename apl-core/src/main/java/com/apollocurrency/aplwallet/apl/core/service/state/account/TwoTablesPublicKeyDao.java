/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.dao.state.account.AccountTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class TwoTablesPublicKeyDao implements PublicKeyDao {
    private final EntityDbTableInterface<PublicKey> publicKeyTable;
    private final EntityDbTableInterface<PublicKey> genesisPublicKeyTable;

    @Inject
    public TwoTablesPublicKeyDao(@Named("publicKeyTable") EntityDbTableInterface<PublicKey> publicKeyTable,
                                 @Named("genesisPublicKeyTable") EntityDbTableInterface<PublicKey> genesisPublicKeyTable) {
        this.publicKeyTable = publicKeyTable;
        this.genesisPublicKeyTable = genesisPublicKeyTable;
    }


    @Override
    public PublicKey searchAll(long id) {
        DbKey dbKey = AccountTable.newKey(id);
        PublicKey publicKey = publicKeyTable.get(dbKey);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey);
        }
        return publicKey;
    }

    @Override
    public PublicKey get(long id) {
        return publicKeyTable.get(AccountTable.newKey(id));
    }

    @Override
    public void insertGenesis(PublicKey publicKey) {
        genesisPublicKeyTable.insert(publicKey);
    }

    @Override
    public void insert(PublicKey publicKey) {
        publicKeyTable.insert(publicKey);
    }

    @Override
    public void truncate() {
        publicKeyTable.truncate();
        genesisPublicKeyTable.truncate();
    }

    @Override
    public PublicKey getByHeight(long id, int height) {
        DbKey dbKey = AccountTable.newKey(id);
        PublicKey publicKey = publicKeyTable.get(dbKey, height);
        if (publicKey == null) {
            publicKey = genesisPublicKeyTable.get(dbKey, height);
        }
        return publicKey;
    }

    @Override
    public List<PublicKey> getAll(int from, int to) {
        return CollectionUtil.toList(publicKeyTable.getAll(from, to));
    }

    @Override
    public List<PublicKey> getAllGenesis(int from, int to) {
        return CollectionUtil.toList(genesisPublicKeyTable.getAll(from, to));
    }

    @Override
    public int genesisCount() {
        return genesisPublicKeyTable.getCount();
    }

    @Override
    public int count() {
        return publicKeyTable.getCount();
    }
}
