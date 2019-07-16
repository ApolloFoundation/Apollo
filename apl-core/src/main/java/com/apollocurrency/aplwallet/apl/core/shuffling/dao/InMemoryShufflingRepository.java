/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.db.InMemoryVersionedDerivedEntityRepository;
import com.apollocurrency.aplwallet.apl.core.shuffling.model.Shuffling;
import com.apollocurrency.aplwallet.apl.core.shuffling.service.Stage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class InMemoryShufflingRepository extends InMemoryVersionedDerivedEntityRepository<Shuffling> implements ShufflingRepository{
    private ShufflingKeyFactory shufflingKeyFactory;
    private static final Comparator<Shuffling> DEFAULT_COMPARATOR = Comparator.comparing(Shuffling::getId);
    private static final Comparator<Shuffling> FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR = (o1, o2) -> {
        // blocks_remaining asc nulls last
        int diff = Integer.compare(o1.getBlocksRemaining(), o2.getBlocksRemaining());
        if (diff != 0) {
            if (o1.getBlocksRemaining() == 0) {
                return 1;
            } else if (o2.getBlocksRemaining() == 0) {
                return -1;
            }
        }
        // height desc
        diff = Integer.compare(o2.getHeight(), o1.getHeight());
        if (diff == 0) {
            // db_id asc
            diff = Long.compare(o1.getDbId(), o2.getDbId());
        }
        return diff;
    };

    @Inject
    public InMemoryShufflingRepository(ShufflingKeyFactory keyFactory) {
        super(keyFactory);
        this.shufflingKeyFactory = keyFactory;
    }

    public Shuffling getCopy(long id) {
        return getCopy(shufflingKeyFactory.newKey(id));
    }

    @Override
    public int getCount() {
        return inReadLock(() -> (int) latestStream().count());
    }

    @Override
    public int getActiveCount() {
        return inReadLock(()-> (int)
                latestStream()
                .filter(s -> s.getBlocksRemaining() > 0)
                .count());
    }

    @Override
    public List<Shuffling> extractAll(int from, int to) {
        return getAll(FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR, from, to);
    }

    @Override
    public List<Shuffling> getActiveShufflings(int from, int to) {
        return inReadLock(()-> latestStream()
                .filter(s -> s.getBlocksRemaining() > 0) // skip finished
                .sorted(Comparator.comparing(Shuffling::getBlocksRemaining)  // order by blocks_remaining asc and then by height desc
                        .thenComparing(Comparator.comparing(Shuffling::getHeight).reversed()))
                .skip(from)
                .limit(to - from)
                .map(Shuffling::deepCopy) // copy shuffling to ensure that returned instance changes will not affect stored shufflings
                .collect(Collectors.toList()));
    }

    @Override
    public List<Shuffling> getFinishedShufflings(int from, int to) {
        return inReadLock(()-> latestStream()
                .filter(s -> s.getBlocksRemaining() <= 0) // include only finished
                .sorted(Comparator.comparing(Shuffling::getHeight).reversed().thenComparing(Shuffling::getDbId))
                .skip(from)
                .limit(to - from)
                .collect(Collectors.toList()));
    }

    @Override
    public Shuffling get(long shufflingId) {
        return get(shufflingKeyFactory.newKey(shufflingId));
    }

    @Override
    public int getHoldingShufflingCount(long holdingId, boolean includeFinished) {
        return inReadLock(()-> {
            Stream<Shuffling> shufflingStream = latestStream()
                    .filter(s -> s.getHoldingId() == holdingId);
            if (!includeFinished) {
                shufflingStream = shufflingStream.filter(s -> s.getBlocksRemaining() > 0);
            }
            return (int) shufflingStream.count();
        });
    }

    @Override
    public List<Shuffling> getHoldingShufflings(long holdingId, Stage stage, boolean includeFinished, int from, int to) {
        return inReadLock(()-> {
            Stream<Shuffling> shufflingStream = latestStream()
                    .filter(s -> s.getHoldingId() == holdingId);
            if (!includeFinished) {
                shufflingStream = shufflingStream.filter(s -> s.getBlocksRemaining() > 0);
            }
            if (stage != null) {
                shufflingStream = shufflingStream.filter(s -> s.getStage() == stage);
            }

            return shufflingStream
                    .sorted(FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR)
                    .skip(from)
                    .limit(to - from)
                    .collect(Collectors.toList());
        });
    }

    @Override
    public List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return inReadLock(()->
            latestStream()
                    .filter(s -> s.getAssigneeAccountId() == assigneeAccountId && s.getStage() == Stage.PROCESSING)
                    .sorted(FINISHED_LAST_BLOCKS_REMAINING_ASC_HEIGHT_DESC_COMPARATOR)
                    .skip(from)
                    .limit(to - from)
                    .collect(Collectors.toList())
        );
    }

    private Stream<Shuffling> latestStream() {
        return getAllEntities().values()
                .stream()
                .filter(l -> l.get(l.size() - 1).isLatest()) // skip deleted
                .map(l -> l.get(l.size() - 1)); // get latest
    }
}
