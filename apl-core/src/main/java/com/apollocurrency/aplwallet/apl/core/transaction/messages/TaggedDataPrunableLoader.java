/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;

public abstract class TaggedDataPrunableLoader<T extends TaggedDataAttachment> implements PrunableLoader<T> {
    protected final PrunableService prunableService;
    protected final TaggedDataService taggedDataService;

    public TaggedDataPrunableLoader(PrunableService prunableService, TaggedDataService taggedDataService) {
        this.prunableService = prunableService;
        this.taggedDataService = taggedDataService;
    }

    @Override
    public void loadPrunable(Transaction transaction, T appendix, boolean includeExpiredPrunable) {
        if (appendix.getData() == null && appendix.getTaggedData() == null && prunableService.shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            appendix.setTaggedData(taggedDataService.getData(appendix.getTaggedDataId(transaction)));
        }
    }
}
