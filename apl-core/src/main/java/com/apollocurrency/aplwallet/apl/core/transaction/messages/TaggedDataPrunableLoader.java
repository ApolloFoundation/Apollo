/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;

public abstract class TaggedDataPrunableLoader<T extends TaggedDataAttachment> implements PrunableLoader<T> {
    protected final PrunableLoadingChecker loadingChecker;
    protected final TaggedDataService taggedDataService;

    public TaggedDataPrunableLoader(PrunableLoadingChecker loadingChecker, TaggedDataService taggedDataService) {
        this.loadingChecker = loadingChecker;
        this.taggedDataService = taggedDataService;
    }

    @Override
    public void loadPrunable(Transaction transaction, T appendix, boolean includeExpiredPrunable) {
        if (appendix.getData() == null && appendix.getTaggedData() == null && loadingChecker.shouldLoadPrunable(transaction, includeExpiredPrunable)) {
            appendix.setTaggedData(taggedDataService.getData(appendix.getTaggedDataId(transaction)));
        }
    }
}
