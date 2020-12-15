/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.prunable;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.converter.db.tagged.TaggedDataMapper;
import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.PrunableDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * We will not store files in transaction attachment, alternatively we will store it only in tagged_data table
 * and change default behavior for rollback on scan to do not clear values, which cannot be restored
 */
@Singleton
@DatabaseSpecificDml(DmlMarker.FULL_TEXT_SEARCH)
public class TaggedDataTable extends PrunableDbTable<TaggedData> implements SearchableTableInterface<TaggedData> {

    private static final String DB_TABLE = "tagged_data";
    private static final String FULL_TEXT_SEARCH_COLUMNS = "name,description,tags";
    private static final LongKeyFactory<TaggedData> taggedDataKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(TaggedData taggedData) {
            return newKey(taggedData.getId());
        }
    };
    private DataTagDao dataTagDao;
    private BlockchainConfig blockchainConfig;
    private TimeService timeService;
    private TaggedDataMapper MAPPER = new TaggedDataMapper();

    @Inject
    public TaggedDataTable(DataTagDao dataTagDao,
                           BlockchainConfig blockchainConfig,
                           TimeService timeService,
                           DerivedTablesRegistry derivedDbTablesRegistry,
                           DatabaseManager databaseManager,
                           FullTextConfig fullTextConfig,
                           PropertiesHolder propertiesHolder,
                           Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(DB_TABLE, taggedDataKeyFactory, true, FULL_TEXT_SEARCH_COLUMNS,
            derivedDbTablesRegistry, databaseManager, fullTextConfig, blockchainConfig, propertiesHolder, deleteOnTrimDataEvent);
        this.dataTagDao = dataTagDao;
        this.timeService = timeService;
        this.blockchainConfig = blockchainConfig;
    }

    public static DbClause getDbClause(String channel, long accountId) {
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
    public TaggedData load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        TaggedData taggedData = MAPPER.map(rs, null);
        taggedData.setDbKey(dbKey);
        return taggedData;
    }

    public DbKey newKey(long taggedDataId) {
        return taggedDataKeyFactory.newKey(taggedDataId);
    }

    @Override
    public String defaultSort() {
        return " ORDER BY block_timestamp DESC, height DESC, db_id DESC ";
    }

    @Override
    public void prune(int time) {
        if (blockchainConfig.isEnablePruning()) {
            try (Connection con = getDatabaseManager().getDataSource().getConnection();
                 PreparedStatement pstmtSelect = con.prepareStatement("SELECT parsed_tags "
                     + "FROM tagged_data WHERE transaction_timestamp < ? AND latest = TRUE ")) {
                int expiration = timeService.getEpochTime() - blockchainConfig.getMaxPrunableLifetime();
                pstmtSelect.setInt(1, expiration);
                Map<String, Integer> expiredTags = new HashMap<>();
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    while (rs.next()) {
                        Object[] array = (Object[]) rs.getArray("parsed_tags").getArray();
                        for (Object tag : array) {
                            Integer count = expiredTags.get(tag);
                            expiredTags.put((String) tag, count != null ? count + 1 : 1);
                        }
                    }
                }
                dataTagDao.delete(expiredTags);
            } catch (SQLException e) {
                throw new RuntimeException(e.toString(), e);
            }
        }
        super.prune(time);
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

    @Override
    public void save(Connection con, TaggedData taggedData) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO tagged_data (id, account_id, `name`, description, tags, parsed_tags, "
                + "`type`, channel, `data`, is_text, filename, block_timestamp, transaction_timestamp, height, latest) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TRUE) "
                + "ON DUPLICATE KEY UPDATE id = VALUES(id), account_id = VALUES(account_id), `name` = VALUES(`name`), "
                + "description = VALUES(description), tags = VALUES(tags), parsed_tags = VALUES(parsed_tags), "
                + "`type` = VALUES(`type`), channel = VALUES(channel), `data` = VALUES(`data`), is_text = VALUES(is_text), "
                + "filename = VALUES(filename), block_timestamp = VALUES(block_timestamp), "
                + "transaction_timestamp = VALUES(transaction_timestamp), height = VALUES(height), latest = TRUE",
                Statement.RETURN_GENERATED_KEYS)
        ) {
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
            try (final ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    taggedData.setDbId(rs.getLong(1));
                }
            }
        }
    }

    @Override
    public boolean isScanSafe() {
        return false; // tagged data stored only in this table, taggedDataAttachment in transaction contains only hash, so we should not rollback it during scan
    }

    public boolean isPruned(long transactionId) {
        try (Connection con = databaseManager.getDataSource().getConnection();
             PreparedStatement pstmt = con.prepareStatement("SELECT 1 FROM tagged_data WHERE id = ?")) {
            pstmt.setLong(1, transactionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return !rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public DbIterator<TaggedData> searchData(String query, String channel, long accountId, int from, int to) {
//        return search(query, getDbClause(channel, accountId), from, to,
//            " ORDER BY ft.score DESC, tagged_data.block_timestamp DESC, tagged_data.db_id DESC ");
        throw new UnsupportedOperationException("Call service, should be implemented by service");
    }

    @Override
    public final DbIterator<TaggedData> search(String query, DbClause dbClause, int from, int to) {
//        return search(query, dbClause, from, to, " ORDER BY ft.score DESC ");
        throw new UnsupportedOperationException("Call service, should be implemented by service");
    }

    @Override
    public final DbIterator<TaggedData> search(String query, DbClause dbClause, int from, int to, String sort) {
        throw new UnsupportedOperationException("Call service, should be implemented by service");
    }

}
