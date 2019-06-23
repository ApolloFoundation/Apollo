/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.dao;

import com.apollocurrency.aplwallet.apl.core.app.EpochTime;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.tagged.mapper.TaggedDataMapper;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
/**
 * We will not store files in transaction attachment, alternatively we will store it only in tagged_data table
 * and change default behavior for rollback on scan to do not clear values, which cannot be restored
 */
public class TaggedDataDao extends PrunableDbTable<TaggedData> {

    private DataTagDao dataTagDao;
    protected DatabaseManager databaseManager;
    private BlockchainConfig blockchainConfig;
    private static volatile EpochTime timeService = CDI.current().select(EpochTime.class).get();

    private static final String DB_TABLE = "tagged_data";
    private static final String FULL_TEXT_SEARCH_COLUMNS = "name,description,tags";
    private TaggedDataMapper MAPPER = new TaggedDataMapper();

    private static final LongKeyFactory<TaggedData> taggedDataKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(TaggedData taggedData) {
            return newKey(taggedData.getId());
        }
    };

    @Inject
    public TaggedDataDao(DataTagDao dataTagDao, DatabaseManager databaseManager,
                         BlockchainConfig blockchainConfig) {
        super(DB_TABLE, taggedDataKeyFactory, true, FULL_TEXT_SEARCH_COLUMNS, false);
        this.dataTagDao = dataTagDao;
        this.databaseManager = databaseManager;
        this.blockchainConfig = blockchainConfig;
    }

    private TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }

    @Override
    public TaggedData load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        TaggedData taggedData = MAPPER.map(rs, null);
        taggedData.setDbKey(dbKey);
        return taggedData;
    }

/*
    public void save(TaggedData taggedData) throws SQLException {
        save(lookupDataSource().getConnection(), taggedData);
    }
*/

    public DbKey newKey(long taggedDataId) {
        return taggedDataKeyFactory.newKey(taggedDataId);
    }

    @Override
    protected String defaultSort() {
        return " ORDER BY block_timestamp DESC, height DESC, db_id DESC ";
    }

    @Override
    protected void prune() {
        if (blockchainConfig.isEnablePruning()) {
            try (Connection con = lookupDataSource().getConnection();
                 PreparedStatement pstmtSelect = con.prepareStatement("SELECT parsed_tags "
                         + "FROM tagged_data WHERE transaction_timestamp < ? AND latest = TRUE ")) {
                int expiration = timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime();
                pstmtSelect.setInt(1, expiration);
                Map<String, Integer> expiredTags = new HashMap<>();
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    while (rs.next()) {
                        Object[] array = (Object[])rs.getArray("parsed_tags").getArray();
                        for (Object tag : array) {
                            Integer count = expiredTags.get(tag);
                            expiredTags.put((String)tag, count != null ? count + 1 : 1);
                        }
                    }
                }
                dataTagDao.delete(expiredTags);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
        super.prune();
    }

    @Override
    public int getCount() {
        return super.getCount();
    }

    @Override
    public DbIterator<TaggedData> getAll(int from, int to) {
        return super.getAll(from, to);
    }

    public TaggedData getData(long transactionId) {
        return super.get(taggedDataKeyFactory.newKey(transactionId));
    }

    public DbIterator<TaggedData> getData(String channel, long accountId, int from, int to) {
        if (channel == null && accountId == 0) {
            throw new IllegalArgumentException("Either channel, or accountId, or both, must be specified");
        }
        return super.getManyBy(getDbClause(channel, accountId), from, to);
    }

    public DbIterator<TaggedData> searchData(String query, String channel, long accountId, int from, int to) {
        return super.search(query, getDbClause(channel, accountId), from, to,
                " ORDER BY ft.score DESC, tagged_data.block_timestamp DESC, tagged_data.db_id DESC ");
    }

    private static DbClause getDbClause(String channel, long accountId) {
        DbClause dbClause = DbClause.EMPTY_CLAUSE;
        if (channel != null) {
            dbClause = new DbClause.StringClause("channel", channel);
        }
        if (accountId != 0) {
            DbClause accountClause = new DbClause.LongClause("account_id", accountId);
            dbClause = dbClause != DbClause.EMPTY_CLAUSE ? dbClause.and(accountClause) : accountClause;
        }
        return dbClause;
    }

    @Override
    public void save(Connection con, TaggedData taggedData) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("MERGE INTO tagged_data (id, account_id, name, description, tags, parsed_tags, "
                + "type, channel, data, is_text, filename, block_timestamp, transaction_timestamp, height, latest) "
                + "KEY (id, height) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, taggedData.getId());
            pstmt.setLong(++i, taggedData.getAccountId());
            pstmt.setString(++i, taggedData.getName());
            pstmt.setString(++i, taggedData.getDescription());
            pstmt.setString(++i, taggedData.getTags());
            DbUtils.setArray(pstmt, ++i, taggedData.getParsedTags());
            pstmt.setString(++i, taggedData.getType());
            pstmt.setString(++i, taggedData.getChannel());
            pstmt.setBytes(++i, taggedData.getData());
            pstmt.setBoolean(++i, taggedData.isText());
            pstmt.setString(++i, taggedData.getFilename());
            pstmt.setInt(++i, taggedData.getBlockTimestamp());
            pstmt.setInt(++i, taggedData.getTransactionTimestamp());
            pstmt.setInt(++i, taggedData.getHeight());
            pstmt.executeUpdate();
        }
    }


/*
    public void add(TransactionImpl transaction, TaggedDataUploadAttachment attachment) {
        if (timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMaxPrunableLifetime() && attachment.getData() != null) {
            DbKey dbKey = keyFactory.newKey(transaction.getId());
            TaggedData taggedData = this.get(dbKey);
            if (taggedData == null) {
                taggedData = new TaggedData(transaction, attachment,
                        blockchain.getLastBlockTimestamp(), blockchain.getHeight());
                this.insert(taggedData);
//                DataTag.add(taggedData); // TODO: YL review and change
            }
        }
        TaggedDataTimestamp timestamp = new TaggedDataTimestamp(transaction.getId(), transaction.getTimestamp());
        taggedDataTimestampDao.insert(timestamp);
    }
*/

/*
    public void extend(Transaction transaction, TaggedDataExtendAttachment attachment) {
        long taggedDataId = attachment.getTaggedDataId();
        DbKey dbKey = taggedDataKeyFactory.newKey(taggedDataId);
        TaggedDataTimestamp timestamp = taggedDataTimestampDao.get(dbKey);
        if (transaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp.getTimestamp()) {
            timestamp.setTimestamp(transaction.getTimestamp() );
        } else {
            timestamp.setTimestamp(timestamp.getTimestamp()
                    + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp.getTimestamp()));
        }
        taggedDataTimestampDao.insert(timestamp);

//        List<Long> extendTransactionIds = taggedDataExtendDao.get(dbKey);
        List<TaggedDataExtend> taggedDataExtendList = taggedDataExtendDao.get(dbKey);
        List<Long> extendTransactionIds = taggedDataExtendList // TODO: YL review
                .stream().map(TaggedDataExtend::getId).collect(Collectors.toList());
        extendTransactionIds.add(transaction.getId());
        taggedDataExtendDao.insert(taggedDataExtendList);
        if (timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime() < timestamp.getTimestamp()) {
            TaggedData taggedData = this.get(dbKey);
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
                this.insert(taggedData);
            }
        }
    }
*/

    @Override
    public boolean isScanSafe() {
        return false; // tagged data stored only in this table, taggedDataAttachment in transaction contains only hash, so we should not rollback it during scan
    }

   /*
    public void restore(Transaction transaction, TaggedDataUploadAttachment attachment, int blockTimestamp, int height) {
        TaggedData taggedData = new TaggedData(transaction, attachment, blockTimestamp, height);
        this.insert(taggedData);
        dataTagDao.add(taggedData, height);
        int timestamp = transaction.getTimestamp();// TODO: YL review
//        for (long extendTransactionId : taggedDataExtendDao.getExtendTransactionIds(transaction.getId())) {
        for (TaggedDataExtend taggedDataForTransaction : taggedDataExtendDao.getExtendTransactionIds(transaction.getId())) {
//            Transaction extendTransaction = blockchain.getTransaction(extendTransactionId);
            Transaction extendTransaction = blockchain.getTransaction(taggedDataForTransaction.getExtendId());
            if (extendTransaction.getTimestamp() - blockchainConfig.getMinPrunableLifetime() > timestamp) {
                timestamp = extendTransaction.getTimestamp();
            } else {
                timestamp = timestamp + Math.min(blockchainConfig.getMinPrunableLifetime(), Integer.MAX_VALUE - timestamp);
            }
            taggedData.setTransactionTimestamp(timestamp);
            taggedData.setBlockTimestamp(extendTransaction.getBlockTimestamp());
            taggedData.setHeight(extendTransaction.getHeight());
            this.insert(taggedData);
        }
    }

*/

    public boolean isPruned(long transactionId) {
        try (Connection con = lookupDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM tagged_data WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

}
