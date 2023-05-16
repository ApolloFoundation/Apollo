/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;

import java.util.List;

public interface ShufflingRepository extends EntityDbTableInterface<Shuffling> {
    LongKeyFactory<Shuffling> dbKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(Shuffling shuffling) {
            if (shuffling.getDbKey() == null) {
                shuffling.setDbKey(dbKeyFactory.newKey(shuffling.getId()));
            }
            return shuffling.getDbKey();
        }
    };

    int getCount();

    int getActiveCount();

    List<Shuffling> extractAll(int from, int to);

    List<Shuffling> getActiveShufflings(int from, int to);

    List<Shuffling> getFinishedShufflings(int from, int to);

    Shuffling get(long shufflingId);

    int getHoldingShufflingCount(long holdingId, boolean includeFinished);

    List<Shuffling> getHoldingShufflings(long holdingId, ShufflingStage stage, boolean includeFinished, int from, int to);

    List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to);

    void insert(Shuffling shuffling);

    boolean delete(Shuffling shuffling);

    List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to);
}