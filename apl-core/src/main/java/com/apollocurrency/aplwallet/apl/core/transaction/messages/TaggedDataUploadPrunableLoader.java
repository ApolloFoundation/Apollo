/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaggedDataUploadPrunableLoader extends TaggedDataPrunableLoader<TaggedDataUploadAttachment>{

    @Inject
    public TaggedDataUploadPrunableLoader(PrunableService prunableService, TaggedDataService taggedDataService) {
        super(prunableService, taggedDataService);
    }

    @Override
    public void restorePrunableData(Transaction transaction, TaggedDataUploadAttachment appendix, int blockTimestamp, int height) {
        taggedDataService.restore(transaction, appendix, blockTimestamp, height);
    }
}
