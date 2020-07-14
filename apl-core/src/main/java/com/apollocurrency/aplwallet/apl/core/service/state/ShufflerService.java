/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.Shuffler;
import com.apollocurrency.aplwallet.apl.core.exception.ShufflerException;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ShufflerService {

    Shuffler addOrGetShuffler(byte[] secretBytes, byte[] recipientPublicKey, byte[] shufflingFullHash) throws ShufflerException;

    List<Shuffler> getAllShufflers();

    List<Shuffler> getShufflingShufflers(byte[] shufflingFullHash);

    List<Shuffler> getAccountShufflers(long accountId);

    Shuffler getShuffler(long accountId, byte[] shufflingFullHash);

    Shuffler stopShuffler(long accountId, byte[] shufflingFullHash);

    void stopAllShufflers();

    Map<String, Map<Long, Shuffler>> getShufflingsMap();

    void removeShufflingsByHash(String hash);

    Map<Integer, Set<String>> getExpirations();

    void removeExpirationsByHeight(Integer height);

}
