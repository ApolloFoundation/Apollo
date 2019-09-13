/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedData;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;

public interface TaggedDataService {

    void add(Transaction transaction, TaggedDataUploadAttachment attachment);

    void extend(Transaction transaction, TaggedDataExtendAttachment attachment);

    void restore(Transaction transaction, TaggedDataUploadAttachment attachment, int blockTimestamp, int height);

    boolean isPruned(long transactionId);

    int getTaggedDataCount();

    TaggedData getData(long transactionId);

    DbIterator<TaggedData> getData(String channel, long accountId, int from, int to);

    DbIterator<TaggedData> searchData(String query, String channel, long accountId, int from, int to);

    DbIterator<TaggedData> getAll(int from, int to);

    int getDataTagCount();

    DbIterator<DataTag> getAllTags(int from, int to);

    DbIterator<DataTag> getTagsLike(String prefix, int from, int to);

    List<TaggedDataExtend> getExtendTransactionIds(long taggedDataId);

}
