/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.tagged.dao.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedData;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;

import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TaggedDataServiceImpl implements TaggedDataService {

    private BlockchainConfig blockchainConfig;
    private Blockchain blockchain;
    private TaggedDataDao taggedDataDao;
    private DataTagDao dataTagDao;
    private TaggedDataTimestampDao taggedDataTimestampDao;
    private TaggedDataExtendDao taggedDataExtendDao;
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();

    private static LongKeyFactory<UnconfirmedTransaction> keyFactory;// = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>(){}).get();

    @Inject
    public TaggedDataServiceImpl(TaggedDataDao taggedDataDao, DataTagDao dataTagDao,
                                 BlockchainConfig blockchainConfig, Blockchain blockchain,
                                 TaggedDataTimestampDao taggedDataTimestampDao, TaggedDataExtendDao taggedDataExtendDao) {
        this.taggedDataDao = taggedDataDao;
        this.dataTagDao = dataTagDao;
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.taggedDataTimestampDao = taggedDataTimestampDao;
        this.taggedDataExtendDao = taggedDataExtendDao;
    }

    @Override
    public void add(Transaction transaction, TaggedDataUploadAttachment attachment) {
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime() && attachment.getData() != null) {
            if (keyFactory == null) {
                keyFactory = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>(){}).get();
            }
            DbKey dbKey = keyFactory.newKey(transaction.getId());
            TaggedData taggedData = taggedDataDao.get(dbKey);
            if (taggedData == null) {
                taggedData = new TaggedData(transaction, attachment,
                        blockchain.getLastBlockTimestamp(), blockchain.getHeight());
                taggedDataDao.insert(taggedData);
                dataTagDao.add(taggedData);
            }
        }
        TaggedDataTimestamp timestamp = new TaggedDataTimestamp(transaction.getId(), transaction.getTimestamp());
        taggedDataTimestampDao.insert(timestamp);
    }

    @Override
    public void extend(Transaction transaction, TaggedDataExtendAttachment attachment) {
        long taggedDataId = attachment.getTaggedDataId();
        DbKey dbKey = taggedDataDao.newKey(taggedDataId);
        TaggedDataTimestamp timestamp = taggedDataTimestampDao.get(dbKey);
        if (transaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp.getTimestamp()) {
            timestamp.setTimestamp(transaction.getTimestamp() );
        } else {
            timestamp.setTimestamp(timestamp.getTimestamp()
                    + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp.getTimestamp()));
        }
        timestamp.setHeight(blockchain.getHeight());
        taggedDataTimestampDao.insert(timestamp);

//        List<Long> extendTransactionIds = taggedDataExtendDao.get(dbKey);
        List<TaggedDataExtend> taggedDataExtendList = taggedDataExtendDao.get(dbKey)
                .stream()
                .peek(e-> e.setHeight(blockchain.getHeight()))
                .collect(Collectors.toList());

        taggedDataExtendList.add(new TaggedDataExtend(taggedDataId, blockchain.getHeight(),transaction.getId()));
        taggedDataExtendDao.insert(taggedDataExtendList);
        if (timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime() < timestamp.getTimestamp()) {
            TaggedData taggedData = taggedDataDao.get(dbKey);
            //TODO check possible error for sharded blockchain
            if (taggedData == null && attachment.getData() != null) {
                Transaction uploadTransaction = blockchain.getTransaction(taggedDataId);
                taggedData = new TaggedData(uploadTransaction, attachment,
                        blockchain.getLastBlockTimestamp(), blockchain.getHeight());
                dataTagDao.add(taggedData);
            }
            if (taggedData != null) {
                taggedData.setTransactionTimestamp(timestamp.getTimestamp());
                taggedData.setBlockTimestamp(blockchain.getLastBlockTimestamp());
                taggedData.setHeight(blockchain.getHeight());
                taggedDataDao.insert(taggedData);
            }
        }
    }

    @Override
    public void restore(Transaction transaction, TaggedDataUploadAttachment attachment, int blockTimestamp, int height) {
        TaggedData taggedData = new TaggedData(transaction, attachment, blockTimestamp, height);
        taggedData.setDbKey(taggedDataDao.newKey(transaction.getId()));
        taggedDataDao.insert(taggedData);
        dataTagDao.add(taggedData, height);
        int timestamp = transaction.getTimestamp();// TODO: YL review
//        for (long extendTransactionId : taggedDataExtendDao.getExtendTransactionIds(transaction.getId())) {
        for (TaggedDataExtend taggedDataForTransaction : taggedDataExtendDao.getExtendTransactionIds(transaction.getId())) {
            Transaction extendTransaction = blockchain.getTransaction(taggedDataForTransaction.getExtendId());
            // TODO: NPE is possible here if 'extendTransaction' not found
            if (extendTransaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp) {
                timestamp = extendTransaction.getTimestamp();
            } else {
                timestamp = timestamp + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp);
            }
            taggedData.setTransactionTimestamp(timestamp);
            taggedData.setBlockTimestamp(extendTransaction.getBlockTimestamp());
//            taggedData.setHeight(extendTransaction.getHeight());
            taggedData.setHeight(blockchain.getHeight()); // TODO: YL review
            taggedDataDao.insert(taggedData);
        }
    }

    @Override
    public boolean isPruned(long transactionId) {
        return taggedDataDao.isPruned(transactionId);
    }

    @Override
    public int getTaggedDataCount() {
        return taggedDataDao.getCount();
    }

    @Override
    public TaggedData getData(long transactionId) {
        return taggedDataDao.getData(transactionId);
    }

    @Override
    public DbIterator<TaggedData> getData(String channel, long accountId, int from, int to) {
        return taggedDataDao.getData(channel, accountId, from, to);
    }

    @Override
    public DbIterator<TaggedData> searchData(String query, String channel, long accountId, int from, int to) {
        return taggedDataDao.searchData(query, channel, accountId, from, to);
    }

    public DbIterator<TaggedData> getAll(int from, int to) {
        return taggedDataDao.getAll(from, to);
    }

    @Override
    public int getDataTagCount() {
        return dataTagDao.getDataTagCount();
    }

    @Override
    public DbIterator<DataTag> getAllTags(int from, int to) {
        return dataTagDao.getAllTags(from, to);
    }

    @Override
    public DbIterator<DataTag> getTagsLike(String prefix, int from, int to) {
        return dataTagDao.getTagsLike(prefix, from, to);
    }

    @Override
    public List<TaggedDataExtend> getExtendTransactionIds(long taggedDataId) {
        return taggedDataExtendDao.getExtendTransactionIds(taggedDataId);
    }
}
