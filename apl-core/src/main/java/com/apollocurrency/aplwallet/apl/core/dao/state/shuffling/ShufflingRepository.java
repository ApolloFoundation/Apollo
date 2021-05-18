/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;

import java.util.List;

public interface ShufflingRepository {
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
}