/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.account;

import com.apollocurrency.aplwallet.apl.core.entity.state.account.PublicKey;

import java.util.List;

public interface PublicKeyDao {

    PublicKey searchAll(long id);

    PublicKey get(long id);

    void insertGenesis(PublicKey publicKey);

    void insert(PublicKey publicKey);

    void truncate();

    List<PublicKey> getAllGenesis(int from, int to);

    PublicKey getByHeight(long id, int height);

    PublicKey getGenesisByHeight(long id, int height);

    List<PublicKey> getAll(int from, int to);

    int genesisCount();

    int count();
}
