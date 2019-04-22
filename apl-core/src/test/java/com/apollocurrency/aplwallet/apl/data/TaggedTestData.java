package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.util.AplException;

public class TaggedTestData {

    private final TransactionTestData td;

    public final TaggedDataTimestamp TagDTsmp_1;
    public final TaggedDataTimestamp TagDTsmp_2;
    public final TaggedDataTimestamp TagDTsmp_3;
    public final TaggedDataTimestamp NOT_SAVED_TagDTsmp;
    public final TaggedDataUploadAttachment NOT_SAVED_TagDTsmp_ATTACHMENT;

    public final DataTag dataTag_1;
    public final DataTag dataTag_2;
    public final DataTag dataTag_3;
    public final DataTag dataTag_4;
    public final DataTag dataTag_NOT_SAVED;

    public TaggedTestData() throws AplException.NotValidException {
        td = new TransactionTestData();
        TagDTsmp_1 = new TaggedDataTimestamp(td.TRANSACTION_3.getId(), td.TRANSACTION_3.getTimestamp(), td.TRANSACTION_3.getHeight());
        TagDTsmp_2 = new TaggedDataTimestamp(td.TRANSACTION_4.getId(), td.TRANSACTION_4.getTimestamp(), td.TRANSACTION_4.getHeight());
        TagDTsmp_3 = new TaggedDataTimestamp(td.TRANSACTION_5.getId(), td.TRANSACTION_5.getTimestamp(), td.TRANSACTION_5.getHeight());
        NOT_SAVED_TagDTsmp = new TaggedDataTimestamp(td.TRANSACTION_7.getId(), td.TRANSACTION_7.getTimestamp(), td.TRANSACTION_7.getHeight());
        NOT_SAVED_TagDTsmp_ATTACHMENT = new TaggedDataUploadAttachment(
                "tst name", "test descr", "tst", "attach type", "chnl1", true,
                "smaple_file_name", new byte[]{0, 16, 24, 56});

        dataTag_1 = new DataTag("abc", td.TRANSACTION_2.getHeight(), 1);
        dataTag_2 = new DataTag("efd", td.TRANSACTION_3.getHeight(), 1);
        dataTag_3 = new DataTag("xyz", td.TRANSACTION_4.getHeight(), 2);
        dataTag_4 = new DataTag("trw", td.TRANSACTION_5.getHeight(), 1);
        dataTag_NOT_SAVED = new DataTag("123", td.TRANSACTION_9.getHeight(), 1);
    }

}
