/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.tagged.dao;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.StringKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedPersistentDbTable;
import com.apollocurrency.aplwallet.apl.core.tagged.mapper.DataTagMapper;
import com.apollocurrency.aplwallet.apl.core.tagged.model.DataTag;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class DataTagDao extends VersionedPersistentDbTable<DataTag> {
    private static Logger logger = LoggerFactory.getLogger(DataTagDao.class);

    protected DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();
    private Blockchain blockchain = CDI.current().select(Blockchain.class).get();

    private static final String DB_TABLE = "data_tag";
    private static final DataTagMapper MAPPER = new DataTagMapper();

    private static final StringKeyFactory<DataTag> tagDbKeyFactory = new StringKeyFactory<>("tag") {
        @Override
        public DbKey newKey(DataTag dataTag) {
           return newKey(dataTag.getTag());
        }
    };

    public DataTagDao() {
        super(DB_TABLE, tagDbKeyFactory);
    }

    public DbKey newDbKey(DataTag dataTag) {
        return tagDbKeyFactory.newKey(dataTag.getTag());
    }

    public void add(TaggedData taggedData) {
        for (String tagValue : taggedData.getParsedTags()) {
            DataTag dataTag = get(tagDbKeyFactory.newKey(tagValue));
            if (dataTag == null) {
                dataTag = new DataTag(tagValue, blockchain.getHeight());
            }
            dataTag.setCount(dataTag.getCount() + 1);
            insert(dataTag);
        }
    }

    public void add(TaggedData taggedData, int height) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE data_tag SET tag_count = tag_count + 1 WHERE tag = ? AND height >= ?")) {
            for (String tagValue : taggedData.getParsedTags()) {
                pstmt.setString(1, tagValue);
                pstmt.setInt(2, height);
                int updated = pstmt.executeUpdate();
                if (updated == 0) {
                    DataTag dataTag = new DataTag(tagValue, height);
                    dataTag.setCount(dataTag.getCount() + 1);
                    insert(dataTag);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public void delete(Map<String, Integer> expiredTags) {
        TransactionalDataSource dataSource = databaseManager.getDataSource();
        try (Connection con = dataSource.getConnection();
             PreparedStatement pstmt = con.prepareStatement("UPDATE data_tag SET tag_count = tag_count - ? WHERE tag = ?");
             PreparedStatement pstmtDelete = con.prepareStatement("DELETE FROM data_tag WHERE tag_count <= 0")) {
            for (Map.Entry<String, Integer> entry : expiredTags.entrySet()) {
                pstmt.setInt(1, entry.getValue());
                pstmt.setString(2, entry.getKey());
                pstmt.executeUpdate();
                logger.debug("Reduced mapTag count for " + entry.getKey() + " by " + entry.getValue());
            }
            int deleted = pstmtDelete.executeUpdate();
            if (deleted > 0) {
                logger.debug("Deleted " + deleted + " tags");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    public int getDataTagCount() {
        return this.getCount();
    }

    public DbIterator<DataTag> getAllTags(int from, int to) {
        return this.getAll(from, to);
    }

    public DbIterator<DataTag> getTagsLike(String prefix, int from, int to) {
        DbClause dbClause = new DbClause.LikeClause("tag", prefix);
        return getManyBy(dbClause, from, to, " ORDER BY tag ");
    }

    @Override
    public DataTag load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        DataTag dataTag = MAPPER.map(rs, null);
        dataTag.setDbKey(dbKey);
        return dataTag;
    }

    @Override
    public void save(Connection con, DataTag dataTag) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement(
                "MERGE INTO data_tag (tag, tag_count, height, latest) "
                        + "KEY (tag, height) VALUES (?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setString(++i, dataTag.getTag());
            pstmt.setInt(++i, dataTag.getCount());
            pstmt.setInt(++i, dataTag.getHeight());
            pstmt.executeUpdate();
        }
    }

    @Override
    public String defaultSort() {
        return " ORDER BY tag_count DESC, tag ASC ";
    }

}
