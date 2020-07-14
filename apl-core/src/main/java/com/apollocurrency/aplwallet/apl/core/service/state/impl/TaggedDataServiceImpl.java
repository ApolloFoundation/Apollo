/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state.impl;

import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.service.state.TaggedDataService;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataUploadAttachment;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.inject.spi.CDI;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class TaggedDataServiceImpl implements TaggedDataService {

    private static LongKeyFactory<UnconfirmedTransaction> keyFactory;// = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>(){}).get();
    private BlockchainConfig blockchainConfig;
    private Blockchain blockchain;
    private TaggedDataDao taggedDataDao;
    private DataTagDao dataTagDao;
    private TaggedDataTimestampDao taggedDataTimestampDao;
    private TaggedDataExtendDao taggedDataExtendDao;
    private TimeService timeService;

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
        log.trace("add TaggedDataUpload: trId = {} / blId={}, height={}, {}",
            transaction.getId(), transaction.getBlockId(), blockchainHeight, attachment);
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime() && attachment.getData() != null) {
            if (keyFactory == null) {
                keyFactory = CDI.current().select(new TypeLiteral<LongKeyFactory<UnconfirmedTransaction>>() {
                }).get();
            }
            DbKey dbKey = keyFactory.newKey(transaction.getId());
            log.trace("add TaggedDataUpload: dbKey = {}", dbKey);
            TaggedData taggedData = taggedDataDao.get(dbKey);
            log.trace("add TaggedDataUpload: isFound = {}", taggedData);
            if (taggedData == null) {
                taggedData = new TaggedData(transaction, attachment,
                    blockchain.getLastBlockTimestamp(), blockchainHeight);
                log.trace("add TaggedDataUpload: insert new = {}", taggedData);
                taggedDataDao.insert(taggedData);
                dataTagDao.add(taggedData);
            } else {
                log.trace("add TaggedDataUpload: skipped = {}", taggedData);
            }
        }
        TaggedDataTimestamp timestamp = new TaggedDataTimestamp(transaction.getId(), transaction.getTimestamp(), blockchainHeight);
        log.trace("add TaggedDataUpload: insert = {}", timestamp);
        taggedDataTimestampDao.insert(timestamp);
    }

    @Override
    public void extend(Transaction transaction, TaggedDataExtendAttachment attachment) {
        int blockchainHeight = blockchain.getHeight();
        log.trace("extend TaggedData: trId = {} / blId={}, height={}, {}",
            transaction.getId(), transaction.getBlockId(), blockchainHeight, attachment);
        long taggedDataId = attachment.getTaggedDataId();
        DbKey dbKey = taggedDataDao.newKey(taggedDataId);
        log.trace("extend TaggedData: dbKey = {}", dbKey);
        TaggedDataTimestamp timestamp = taggedDataTimestampDao.get(dbKey);
        log.trace("extend TaggedData: timestamp = {}", timestamp);
        int timestampInRecord = timestamp.getTimestamp();
        int minPrunableLifetime = blockchainConfig.getMinPrunableLifetime();
        log.trace("extend TaggedData: timestamp cond ? = '{}' (trTs={}, minPrunableLifetime={}, timestampInRecord={})",
            transaction.getTimestamp() - minPrunableLifetime > timestampInRecord,
            transaction.getTimestamp(), minPrunableLifetime, timestampInRecord
        );
        if (transaction.getTimestamp() - minPrunableLifetime > timestampInRecord) {
            timestamp.setTimestamp(transaction.getTimestamp());
        } else {
            timestamp.setTimestamp(timestampInRecord
                + Math.min(minPrunableLifetime, Integer.MAX_VALUE - timestampInRecord));
        }
        timestamp.setHeight(blockchainHeight);
        log.trace("extend TaggedData: insert timestamp = {}", timestamp);
        taggedDataTimestampDao.insert(timestamp);

        List<TaggedDataExtend> taggedDataExtendList = taggedDataExtendDao.get(dbKey)
            .stream()
            .peek(e -> e.setHeight(blockchainHeight))
            .collect(Collectors.toList());
        log.trace("extend TaggedData: taggedDataExtendList = [{}]", taggedDataExtendList.size());
        taggedDataExtendList.add(new TaggedDataExtend(null, blockchainHeight, taggedDataId, transaction.getId()));
        log.trace("extend TaggedData: insert taggedDataExtendList = {}", taggedDataExtendList);
        taggedDataExtendDao.insert(taggedDataExtendList);
        int maxPrunableLifetime = blockchainConfig.getMaxPrunableLifetime();
        int epochTime = timeService.getEpochTime();
        if (epochTime - maxPrunableLifetime < timestampInRecord) {
            TaggedData taggedData = taggedDataDao.get(dbKey);
            if (taggedData != null) {
                taggedData.setTransactionTimestamp(timestampInRecord);
                taggedData.setBlockTimestamp(blockchain.getLastBlockTimestamp());
                taggedData.setHeight(blockchainHeight);
                log.trace("extend TaggedData: insert taggedData = {}", taggedData);
                taggedDataDao.insert(taggedData);
            } else {
                log.trace("extend TaggedData: skipped = {}", taggedData);
            }
        } else {
            log.trace("extend TaggedData: timeStamp skipped ? = '{}' (epochTime={}, maxPrunableLifetime={}, timestampInRecord={})",
                epochTime - maxPrunableLifetime < timestampInRecord,
                epochTime, maxPrunableLifetime, timestampInRecord);
        }
    }

    @Override
    public void restore(Transaction transaction, TaggedDataUploadAttachment attachment, int blockTimestamp, int height) {
        log.trace("restore TaggedData: trId = {} / blId={}, height={}, {}",
            transaction.getId(), transaction.getBlockId(), height, attachment);
        TaggedData taggedData = new TaggedData(transaction, attachment, blockTimestamp, height);
        taggedData.setDbKey(taggedDataDao.newKey(transaction.getId()));
        log.trace("restore TaggedData: insert = {}", taggedData);
        taggedDataDao.insert(taggedData);
        dataTagDao.add(taggedData, height);
        int timestamp = transaction.getTimestamp();
        for (TaggedDataExtend taggedDataForTransaction : taggedDataExtendDao.getExtendTransactionIds(transaction.getId())) {
            Transaction extendTransaction = blockchain.getTransaction(taggedDataForTransaction.getExtendId());
            log.trace("restore TaggedData: extendTransaction = {}", extendTransaction);
            // NPE is possible here if 'extendTransaction' not found
            if (extendTransaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp) {
                timestamp = extendTransaction.getTimestamp();
            } else {
                timestamp = timestamp + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp);
            }
            taggedData.setTransactionTimestamp(timestamp);
            taggedData.setBlockTimestamp(extendTransaction.getBlockTimestamp());
            taggedData.setHeight(extendTransaction.getHeight());
            log.trace("restore TaggedData: taggedData = {}", extendTransaction);
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
