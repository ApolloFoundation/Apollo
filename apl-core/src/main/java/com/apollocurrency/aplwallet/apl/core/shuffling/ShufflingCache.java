/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling;

import com.apollocurrency.aplwallet.apl.core.db.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ShufflingCache extends InMemoryVersionedDerivedEntityRepository<Shuffling> {

    public List<Shuffling> getActiveShufflings() {
        return  getAllEntities().values()
                .stream()
                .map(l -> l.get(l.size() - 1))
                .filter(s -> s.getBlocksRemaining() != 0)
                .sorted(Comparator.comparing(Shuffling::getBlocksRemaining)
                        .thenComparing(Comparator.comparing(Shuffling::getHeight).reversed()))
                .map(Shuffling::deepCopy)
                .collect(Collectors.toList());
    }
}
