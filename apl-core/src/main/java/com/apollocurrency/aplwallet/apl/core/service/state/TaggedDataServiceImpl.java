/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.app.observer.events.TrimEvent;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.DataTagDao;
import com.apollocurrency.aplwallet.apl.core.dao.prunable.TaggedDataTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataExtendDao;
import com.apollocurrency.aplwallet.apl.core.dao.state.tagged.TaggedDataTimestampDao;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.UnconfirmedTransactionEntity;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextSearchUpdater;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtendAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUploadAttachment;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.util.db.DbUtils;
import com.apollocurrency.aplwallet.apl.util.db.TransactionalDataSource;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.event.Event;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig.DEFAULT_SCHEMA;

@Slf4j
@Singleton
public class TaggedDataServiceImpl implements TaggedDataService {
    private static final LongKeyFactory<UnconfirmedTransactionEntity> keyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(UnconfirmedTransactionEntity unconfirmedTransaction) {
            return new LongKey(unconfirmedTransaction.getId());
        }
    };

    private final BlockchainConfig blockchainConfig;
    private final Blockchain blockchain;
    private final TaggedDataTable taggedDataTable;
    private final DataTagDao dataTagDao;
    private final TaggedDataTimestampDao taggedDataTimestampDao;
    private final TaggedDataExtendDao taggedDataExtendDao;
    private final TimeService timeService;
    private final FullTextSearchUpdater fullTextSearchUpdater;
    private final Event<FullTextOperationData> fullTextOperationDataEvent;
    private final FullTextSearchService fullTextSearchService;

    @Inject
    public TaggedDataServiceImpl(TaggedDataTable taggedDataTable,
                                 DataTagDao dataTagDao,
                                 BlockchainConfig blockchainConfig,
                                 Blockchain blockchain,
                                 TaggedDataTimestampDao taggedDataTimestampDao,
                                 TaggedDataExtendDao taggedDataExtendDao,
                                 TimeService timeService,
                                 FullTextSearchUpdater fullTextSearchUpdater,
                                 Event<FullTextOperationData> fullTextOperationDataEvent,
                                 FullTextSearchService fullTextSearchService) {
        this.taggedDataTable = taggedDataTable;
        this.timeService = timeService;
        this.dataTagDao = dataTagDao;
        this.blockchainConfig = blockchainConfig;
        this.blockchain = blockchain;
        this.taggedDataTimestampDao = taggedDataTimestampDao;
        this.taggedDataExtendDao = taggedDataExtendDao;
        this.fullTextSearchUpdater = fullTextSearchUpdater;
        this.fullTextOperationDataEvent = fullTextOperationDataEvent;
        this.fullTextSearchService = fullTextSearchService;
    }

    @Override
    public void add(Transaction transaction, TaggedDataUploadAttachment attachment) {
        int blockchainHeight = blockchain.getHeight();
        log.trace("add TaggedDataUpload: trId = {} / blId={}, height={}, {}",
            transaction.getId(), transaction.getBlockId(), blockchainHeight, attachment);
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime() && attachment.getData() != null) {
            DbKey dbKey = keyFactory.newKey(transaction.getId());
            log.trace("add TaggedDataUpload: dbKey = {}", dbKey);
            TaggedData taggedData = taggedDataTable.get(dbKey);
            log.trace("add TaggedDataUpload: isFound = {}", taggedData);
            if (taggedData == null) {
                taggedData = new TaggedData(transaction, attachment,
                    blockchain.getLastBlockTimestamp(), blockchainHeight);
                log.trace("add TaggedDataUpload: insert new = {}", taggedData);
                taggedDataTable.insert(taggedData);
                dataTagDao.add(taggedData);
                createAndFireFullTextSearchDataEvent(taggedData, FullTextOperationData.OperationType.INSERT_UPDATE);
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
        DbKey dbKey = taggedDataTable.newKey(taggedDataId);
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
            TaggedData taggedData = taggedDataTable.get(dbKey);
            if (taggedData != null) {
                taggedData.setTransactionTimestamp(timestampInRecord);
                taggedData.setBlockTimestamp(blockchain.getLastBlockTimestamp());
                taggedData.setHeight(blockchainHeight);
                log.trace("extend TaggedData: insert taggedData = {}", taggedData);
                taggedDataTable.insert(taggedData);
                createAndFireFullTextSearchDataEvent(taggedData, FullTextOperationData.OperationType.INSERT_UPDATE);
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
        taggedData.setDbKey(taggedDataTable.newKey(transaction.getId()));
        log.trace("restore TaggedData: insert = {}", taggedData);
        taggedDataTable.insert(taggedData);
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
            taggedDataTable.insert(taggedData);
        }
        createAndFireFullTextSearchDataEvent(taggedData, FullTextOperationData.OperationType.INSERT_UPDATE);
    }

    @Override
    public boolean isPruned(long transactionId) {
        return taggedDataTable.isPruned(transactionId);
    }

    @Override
    public int getTaggedDataCount() {
        return taggedDataTable.getCount();
    }

    @Override
    public TaggedData getData(long transactionId) {
        return taggedDataTable.getData(transactionId);
    }

    @Override
    public DbIterator<TaggedData> getData(String channel, long accountId, int from, int to) {
        return taggedDataTable.getData(channel, accountId, from, to);
    }

    @Override
    public DbIterator<TaggedData> searchData(String query, String channel, long accountId, int from, int to) {
        StringBuffer inRangeClause = createDbIdInRangeFromLuceneData(query);
        if (inRangeClause.length() == 2) {
            // no DB_ID were fetched from Lucene index, return empty db iterator
            return DbIterator.EmptyDbIterator();
        }
        DbClause dbClause = TaggedDataTable.getDbClause(channel, accountId);
        String sort = " ORDER BY tagged_data.block_timestamp DESC, tagged_data.db_id DESC ";
        return fetchTaggedDataByParams(from, to, inRangeClause, dbClause, sort);
    }

    public DbIterator<TaggedData> getAll(int from, int to) {
        return taggedDataTable.getAll(from, to);
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

    /**
     * compose db_id list for in (id,..id) SQL luceneQuery
     *
     * @param luceneQuery lucene language luceneQuery pattern
     * @return composed sql luceneQuery part
     */
    private StringBuffer createDbIdInRangeFromLuceneData(String luceneQuery) {
        Objects.requireNonNull(luceneQuery, "luceneQuery is NULL");
        StringBuffer inRange = new StringBuffer("(");
        int index = 0;
        try (ResultSet rs = fullTextSearchService.search("public", taggedDataTable.getTableName(), luceneQuery, Integer.MAX_VALUE, 0)) {
            while (rs.next()) {
                Long DB_ID = rs.getLong("keys");
                if (index == 0) {
                    inRange.append(DB_ID);
                } else {
                    inRange.append(",").append(DB_ID);
                }
                index++;
            }
            inRange.append(")");
            log.debug("{}", inRange.toString());
        } catch (SQLException e) {
            log.error("FTS failed", e);
            throw new RuntimeException(e);
        }
        return inRange;
    }

    public DbIterator<TaggedData> fetchTaggedDataByParams(int from, int to,
                                                          StringBuffer inRangeClause,
                                                          DbClause dbClause,
                                                          String sort) {
        Objects.requireNonNull(inRangeClause, "inRangeClause is NULL");
        Objects.requireNonNull(dbClause, "dbClause is NULL");
        Objects.requireNonNull(sort, "sort is NULL");

        Connection con = null;
        TransactionalDataSource dataSource = taggedDataTable.getDatabaseManager().getDataSource();
        final boolean doCache = dataSource.isInTransaction();
        try {
            con = dataSource.getConnection();
            @DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
            PreparedStatement pstmt = con.prepareStatement(
                // select and load full entities from mariadb using prefetched DB_ID list from lucene
                "SELECT " + taggedDataTable.getTableName() + ".* FROM " + taggedDataTable.getTableName()
                    + " WHERE " + taggedDataTable.getTableName() + ".db_id in " + inRangeClause.toString()
                    + (taggedDataTable.isMultiversion() ? " AND " + taggedDataTable.getTableName() + ".latest = TRUE " : " ")
                    + " AND " + dbClause.getClause() + sort
                    + DbUtils.limitsClause(from, to));
            int i = 0;
            i = dbClause.set(pstmt, ++i);
            DbUtils.setLimits(i, pstmt, from, to);
            return new DbIterator<>(con, pstmt, (connection, rs) -> {
                DbKey dbKey = null;
                if (doCache) {
                    dbKey = taggedDataTable.getDbKeyFactory().newKey(rs);
                }
                return taggedDataTable.load(connection, rs, dbKey);
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    private void createAndFireFullTextSearchDataEvent(TaggedData taggedData, FullTextOperationData.OperationType operationType) {
        FullTextOperationData operationData = new FullTextOperationData(
            DEFAULT_SCHEMA, taggedDataTable.getTableName(), Thread.currentThread().getName());
        operationData.setOperationType(operationType);
        operationData.setDbIdValue(taggedData.getDbId());
        operationData.addColumnData(taggedData.getName()).addColumnData(taggedData.getDescription()).addColumnData(taggedData.getTags());
        // send data into Lucene index component
        log.trace("Put lucene index update data = {}", operationData);
//        fullTextSearchUpdater.putFullTextOperationData(operationData);
        this.fullTextOperationDataEvent.select(new AnnotationLiteral<TrimEvent>() {}).fireAsync(operationData);
    }

}
