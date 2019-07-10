/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.service;

import com.apollocurrency.aplwallet.apl.core.shuffling.exception.ShufflerException;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffler;

import java.util.List;

public interface ShufflerService {
    Shuffler addOrGetShuffler(byte[] secretBytes, byte[] recipientPublicKey, byte[] shufflingFullHash) throws ShufflerException;

    List<Shuffler> getAllShufflers();

    List<Shuffler> getShufflingShufflers(byte[] shufflingFullHash);

    List<Shuffler> getAccountShufflers(long accountId);

    Shuffler getShuffler(long accountId, byte[] shufflingFullHash);

    Shuffler stopShuffler(long accountId, byte[] shufflingFullHash);

    void stopAllShufflers();
}
