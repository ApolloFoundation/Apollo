/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.shuffling;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.FullyCachedTable;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;

import javax.enterprise.inject.Vetoed;
import java.util.List;

@Vetoed
public class ShufflingCachedTable extends FullyCachedTable<Shuffling> implements ShufflingRepository {


    public ShufflingCachedTable(InMemoryShufflingRepository memTableCache, ShufflingTable table) {
        super(memTableCache, table);
    }

    @Override
    public int getActiveCount() {
        return memTable().getActiveCount();
    }

    @Override
    public List<Shuffling> extractAll(int from, int to) {
        return memTable().extractAll(from, to);
    }

    @Override
    public List<Shuffling> getActiveShufflings(int from, int to) {
        return memTable().getActiveShufflings(from, to);
    }

    @Override
    public List<Shuffling> getFinishedShufflings(int from, int to) {
        return memTable().getFinishedShufflings(from, to);
    }

    @Override
    public Shuffling get(long shufflingId) {
        return memTable().get(shufflingId);
    }

    @Override
    public int getHoldingShufflingCount(long holdingId, boolean includeFinished) {
        return memTable().getHoldingShufflingCount(holdingId, includeFinished);
    }

    @Override
    public List<Shuffling> getHoldingShufflings(long holdingId, ShufflingStage stage, boolean includeFinished, int from, int to) {
        return memTable().getHoldingShufflings(holdingId, stage, includeFinished, from, to);
    }

    @Override
    public List<Shuffling> getAssignedShufflings(long assigneeAccountId, int from, int to) {
        return memTable().getAssignedShufflings(assigneeAccountId, from, to);
    }

    @Override
    public boolean delete(Shuffling shuffling) {
        return super.deleteAtHeight(shuffling, shuffling.getHeight());
    }

    @Override
    public List<Shuffling> getAccountShufflings(long accountId, boolean includeFinished, int from, int to) {
        return ((ShufflingTable) table).getAccountShufflings(accountId, includeFinished, from, to);
    }

    private InMemoryShufflingRepository memTable() {
        return (InMemoryShufflingRepository) memTableCache;
    }

}
