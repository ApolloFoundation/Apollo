/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

class InMemoryShufflingRepositoryTest extends ShufflingRepositoryTest {

    @Override
    public ShufflingRepository repository() {
        InMemoryShufflingRepository inMemoryShufflingRepository = new InMemoryShufflingRepository(new ShufflingKeyFactory());
        inMemoryShufflingRepository.putAll(std.all);
        return inMemoryShufflingRepository;
    }
}