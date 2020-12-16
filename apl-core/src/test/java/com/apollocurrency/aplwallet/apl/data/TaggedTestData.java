package com.apollocurrency.aplwallet.apl.data;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUploadAttachment;

public class TaggedTestData {

    public final TaggedData TaggedData_1;
    public final TaggedDataUploadAttachment dataUploadAttachment_1;
    public final TaggedData TaggedData_2;
    public final TaggedDataUploadAttachment dataUploadAttachment_2;
    public final TaggedData TaggedData_3;
    public final TaggedDataUploadAttachment dataUploadAttachment_3;
    public final TaggedData TaggedData_4;
    public final TaggedDataUploadAttachment dataUploadAttachment_4;
    public final TaggedData TaggedData_5;
    public final TaggedDataUploadAttachment dataUploadAttachment_5;
    public final TaggedDataExtendAttachment NOT_SAVED_TagExtend_ATTACHMENT;
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
    private final TransactionTestData td;

    public TaggedTestData() throws AplException.NotValidException {
        td = new TransactionTestData();
        dataUploadAttachment_1 = new TaggedDataUploadAttachment(
            "tag1", "tag1 descr", "tag1,tag2,tag3,tag2,sl", "file test type", null,
            true, "test file name 1", "c11dd7986e".getBytes());
        TaggedData_1 = new TaggedData(td.TRANSACTION_3, dataUploadAttachment_1, td.TRANSACTION_3.getTimestamp(), td.TRANSACTION_3.getHeight());

        dataUploadAttachment_2 = new TaggedDataUploadAttachment(
            "tag2", "tag2 descr", "tag2,tag2,ss", "file test type", null,
            true, "test file name 2", "c11dd7986e".getBytes());
        TaggedData_2 = new TaggedData(td.TRANSACTION_4, dataUploadAttachment_2, td.TRANSACTION_4.getTimestamp(), td.TRANSACTION_4.getHeight());

        dataUploadAttachment_3 = new TaggedDataUploadAttachment(
            "tag3", "tag3 descr", "tag3,tag4,tag3,newtag", "file test type", null,
            true, "test file name 3", "c11d8344588e".getBytes());
        TaggedData_3 = new TaggedData(td.TRANSACTION_5, dataUploadAttachment_3, td.TRANSACTION_5.getTimestamp(), td.TRANSACTION_5.getHeight());

        dataUploadAttachment_4 = new TaggedDataUploadAttachment(
            "tag4", "tag4 descr", "tag3,tag3,tag3,tag2,tag2", "file test type", null,
            true, "test file name 4", "c11d1234589e".getBytes());
        TaggedData_4 = new TaggedData(td.TRANSACTION_7, dataUploadAttachment_4, td.TRANSACTION_7.getTimestamp(), td.TRANSACTION_7.getHeight());

        dataUploadAttachment_5 = new TaggedDataUploadAttachment(
            "tag5", "tag5 descr", "iambatman", "file test type", null,
            true, "test file name 5", "c11d1234586e".getBytes());
        TaggedData_5 = new TaggedData(td.TRANSACTION_8, dataUploadAttachment_5, td.TRANSACTION_8.getTimestamp(), td.TRANSACTION_8.getHeight());

        NOT_SAVED_TagExtend_ATTACHMENT = new TaggedDataExtendAttachment(TaggedData_1);

        TagDTsmp_1 = new TaggedDataTimestamp(td.TRANSACTION_3.getId(), td.TRANSACTION_3.getTimestamp(), td.TRANSACTION_3.getHeight());
        TagDTsmp_2 = new TaggedDataTimestamp(td.TRANSACTION_4.getId(), td.TRANSACTION_4.getTimestamp(), td.TRANSACTION_4.getHeight());
        TagDTsmp_3 = new TaggedDataTimestamp(td.TRANSACTION_5.getId(), td.TRANSACTION_5.getTimestamp(), td.TRANSACTION_5.getHeight());
        NOT_SAVED_TagDTsmp = new TaggedDataTimestamp(td.TRANSACTION_7.getId(), td.TRANSACTION_7.getTimestamp(), td.TRANSACTION_7.getHeight());


        NOT_SAVED_TagDTsmp_ATTACHMENT = new TaggedDataUploadAttachment(
            "tst name", "test descr", "tst", "attach type", "chnl1", true,
            "smaple_file_name", new byte[]{0, 16, 24, 56});


        dataTag_1 = new DataTag("abc", td.TRANSACTION_2.getHeight(), 1);
        dataTag_1.setDbId(10);
        dataTag_1.setLatest(true);
        dataTag_2 = new DataTag("efd", td.TRANSACTION_3.getHeight(), 1);
        dataTag_2.setDbId(20);
        dataTag_2.setLatest(false);
        dataTag_3 = new DataTag("xyz", td.TRANSACTION_4.getHeight(), 2);
        dataTag_3.setDbId(30);
        dataTag_3.setLatest(false);
        dataTag_4 = new DataTag("trw", td.TRANSACTION_5.getHeight(), 1);
        dataTag_4.setDbId(40);
        dataTag_4.setLatest(true);
        dataTag_NOT_SAVED = new DataTag("123", td.TRANSACTION_9.getHeight(), 1);
        dataTag_NOT_SAVED.setLatest(true);
        dataTag_NOT_SAVED.setDbId(dataTag_4.getDbId() + 1); // incorrect assumption for mariadb
    }

}
