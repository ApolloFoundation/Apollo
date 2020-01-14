/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
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
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class TaggedDataServiceImpl implements TaggedDataService {

    private BlockchainConfig blockchainConfig;
    private Blockchain blockchain;
    private TaggedDataDao taggedDataDao;
    private DataTagDao dataTagDao;
    private TaggedDataTimestampDao taggedDataTimestampDao;
    private TaggedDataExtendDao taggedDataExtendDao;
    private TimeService timeService;

    private static LongKeyFactory<UnconfirmedTransaction> keyFactory;// = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>(){}).get();

    @Inject
    public TaggedDataServiceImpl(TaggedDataDao taggedDataDao, DataTagDao dataTagDao,
                                 BlockchainConfig blockchainConfig, Blockchain blockchain,
                                 TaggedDataTimestampDao taggedDataTimestampDao, TaggedDataExtendDao taggedDataExtendDao, TimeService timeService) {
        this.taggedDataDao = taggedDataDao;
        this.timeService = timeService;
        this.dataTagDao = dataTagDao;
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.taggedDataTimestampDao = taggedDataTimestampDao;
        this.taggedDataExtendDao = taggedDataExtendDao;
    }

    @Override
    public void add(Transaction transaction, TaggedDataUploadAttachment attachment) {
        int blockchainHeight = blockchain.getHeight();
        log.debug("add TaggedDataUpload: trId = {} / blId={}, height={}, {}",
            transaction.getId(), transaction.getBlockId(), blockchainHeight, attachment);
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime() && attachment.getData() != null) {
            if (keyFactory == null) {
                keyFactory = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>(){}).get();
            }
            DbKey dbKey = keyFactory.newKey(transaction.getId());
            log.debug("add TaggedDataUpload: dbKey = {}", dbKey);
            TaggedData taggedData = taggedDataDao.get(dbKey);
            log.debug("add TaggedDataUpload: isFound = {}", taggedData);
            if (taggedData == null) {
                taggedData = new TaggedData(transaction, attachment,
                        blockchain.getLastBlockTimestamp(), blockchainHeight);
                log.debug("add TaggedDataUpload: insert new = {}", taggedData);
                taggedDataDao.insert(taggedData);
                dataTagDao.add(taggedData);
            } else {
                log.debug("add TaggedDataUpload: skipped = {}", taggedData);
            }
        }
        TaggedDataTimestamp timestamp = new TaggedDataTimestamp(transaction.getId(), transaction.getTimestamp(), blockchainHeight);
        log.debug("add TaggedDataUpload: insert = {}", timestamp);
        taggedDataTimestampDao.insert(timestamp);
    }

    @Override
    public void extend(Transaction transaction, TaggedDataExtendAttachment attachment) {
        int blockchainHeight = blockchain.getHeight();
        log.debug("extend TaggedData: trId = {} / blId={}, height={}, {}",
            transaction.getId(), transaction.getBlockId(), blockchainHeight, attachment);
        long taggedDataId = attachment.getTaggedDataId();
        DbKey dbKey = taggedDataDao.newKey(taggedDataId);
        log.debug("extend TaggedData: dbKey = {}", dbKey);
        TaggedDataTimestamp timestamp = taggedDataTimestampDao.get(dbKey);
        log.debug("extend TaggedData: timestamp = {}", timestamp);
        int timestampInRecord = timestamp.getTimestamp();
        int minPrunableLifetime = blockchainConfig.getMinPrunableLifetime();
        log.debug("extend TaggedData: timestamp cond ? = '{}' (trTs={}, minPrunableLifetime={}, timestampInRecor={})",
            transaction.getTimestamp() - minPrunableLifetime > timestampInRecord,
            transaction.getTimestamp(), minPrunableLifetime, timestampInRecord
            );
        if (transaction.getTimestamp() - minPrunableLifetime > timestampInRecord) {
            timestamp.setTimestamp(transaction.getTimestamp() );
        } else {
            timestamp.setTimestamp(timestampInRecord
                    + Math.min(minPrunableLifetime, Integer.MAX_VALUE - timestampInRecord));
        }
        timestamp.setHeight(blockchainHeight);
        log.debug("extend TaggedData: insert timestamp = {}", timestamp);
        taggedDataTimestampDao.insert(timestamp);

//        List<Long> extendTransactionIds = taggedDataExtendDao.get(dbKey);
        List<TaggedDataExtend> taggedDataExtendList = taggedDataExtendDao.get(dbKey)
                .stream()
                .peek(e-> e.setHeight(blockchainHeight))
                .collect(Collectors.toList());
        log.debug("extend TaggedData: taggedDataExtendList = [{}]", taggedDataExtendList.size());
        taggedDataExtendList.add(new TaggedDataExtend(null, blockchainHeight, taggedDataId, transaction.getId()));
        log.debug("extend TaggedData: insert taggedDataExtendList = {}", taggedDataExtendList);
        taggedDataExtendDao.insert(taggedDataExtendList);
        int maxPrunableLifetime = blockchainConfig.getMaxPrunableLifetime();
        int epochTime = timeService.getEpochTime();
        if (epochTime - maxPrunableLifetime < timestampInRecord) {
            TaggedData taggedData = taggedDataDao.get(dbKey);
            if (taggedData != null) {
                taggedData.setTransactionTimestamp(timestampInRecord);
                taggedData.setBlockTimestamp(blockchain.getLastBlockTimestamp());
                taggedData.setHeight(blockchainHeight);
                log.debug("extend TaggedData: insert taggedData = {}", taggedData);
                taggedDataDao.insert(taggedData);
            } else {
                log.debug("extend TaggedData: skipped = {}", taggedData);
            }
        } else {
            log.debug("extend TaggedData: timeStamp skipped ? = '{}' (epochTime={}, maxPrunableLifetime={}, timestampInRecord={})",
                epochTime - maxPrunableLifetime < timestampInRecord,
                epochTime, maxPrunableLifetime, timestampInRecord);
        }
    }

    @Override
    public void restore(Transaction transaction, TaggedDataUploadAttachment attachment, int blockTimestamp, int height) {
        log.debug("restore TaggedData: trId = {} / blId={}, height={}, {}",
            transaction.getId(), transaction.getBlockId(), height, attachment);
        TaggedData taggedData = new TaggedData(transaction, attachment, blockTimestamp, height);
        taggedData.setDbKey(taggedDataDao.newKey(transaction.getId()));
        log.debug("restore TaggedData: insert = {}", taggedData);
        taggedDataDao.insert(taggedData);
        dataTagDao.add(taggedData, height);
        int timestamp = transaction.getTimestamp();
        for (TaggedDataExtend taggedDataForTransaction : taggedDataExtendDao.getExtendTransactionIds(transaction.getId())) {
            Transaction extendTransaction = blockchain.getTransaction(taggedDataForTransaction.getExtendId());
            log.debug("restore TaggedData: extendTransaction = {}", extendTransaction);
            // NPE is possible here if 'extendTransaction' not found
            if (extendTransaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp) {
                timestamp = extendTransaction.getTimestamp();
            } else {
                timestamp = timestamp + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp);
            }
            taggedData.setTransactionTimestamp(timestamp);
            taggedData.setBlockTimestamp(extendTransaction.getBlockTimestamp());
            taggedData.setHeight(extendTransaction.getHeight());
            log.debug("restore TaggedData: taggedData = {}", extendTransaction);
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
