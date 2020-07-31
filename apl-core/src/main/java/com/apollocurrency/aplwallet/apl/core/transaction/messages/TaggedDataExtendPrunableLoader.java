/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaggedDataExtendPrunableLoader extends TaggedDataPrunableLoader<TaggedDataExtendAttachment> {
    @Inject
    public TaggedDataExtendPrunableLoader(PrunableLoadingChecker loadingChecker, TaggedDataService taggedDataService) {
        super(loadingChecker, taggedDataService);
    }

    @Override
    public void restorePrunableData(Transaction transaction, TaggedDataExtendAttachment appendix, int blockTimestamp, int height) {

    }
}
