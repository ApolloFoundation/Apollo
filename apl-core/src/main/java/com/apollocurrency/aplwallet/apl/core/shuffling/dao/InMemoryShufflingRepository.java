/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.db.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InMemoryShufflingRepository extends InMemoryVersionedDerivedEntityRepository<Shuffling> {
    private ShufflingKeyFactory shufflingKeyFactory;
    @Inject
    public InMemoryShufflingRepository(ShufflingKeyFactory keyFactory) {
        super(keyFactory);
        this.shufflingKeyFactory = keyFactory;
    }

    public Shuffling getCopy(long id) {
        return getCopy(shufflingKeyFactory.newKey(id));
    }

    public List<Shuffling> getActiveShufflings() {
        return  getAllEntities().values()
                .stream()
                .filter(l->l.get(l.size() - 1).isLatest()) // skip deleted
                .map(l -> l.get(l.size() - 1)) // get latest
                .filter(s -> s.getBlocksRemaining() >= 0) // skip finished
                .sorted(Comparator.comparing(Shuffling::getBlocksRemaining)  // order by blocks_remaining asc and then by height desc
                        .thenComparing(Comparator.comparing(Shuffling::getHeight).reversed()))
                .map(Shuffling::deepCopy) // copy shuffling to ensure that returned instance changes will not affect stored shufflings
                .collect(Collectors.toList());
    }
}
