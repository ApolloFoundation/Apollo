/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.prunable;

import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.StringKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.converter.db.tagged.DataTagMapper;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.DataTag;
import com.apollocurrency.aplwallet.apl.core.entity.prunable.TaggedData;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;

import static com.apollocurrency.aplwallet.apl.util.ThreadUtils.last3Stacktrace;

@Singleton
@Slf4j
public class DataTagDao extends EntityDbTable<DataTag> {

    private static final String DB_TABLE = "data_tag";

    private static final StringKeyFactory<DataTag> tagDbKeyFactory = new StringKeyFactory<>("tag") {
        @Override
        public DbKey newKey(DataTag dataTag) {
            return newKey(dataTag.getTag());
        }
    };
    private static final DataTagMapper MAPPER = new DataTagMapper(tagDbKeyFactory);

    @Inject
    public DataTagDao() {
        super(DB_TABLE, tagDbKeyFactory, true, null, false);
    }

    public DbKey newDbKey(DataTag dataTag) {
        return tagDbKeyFactory.newKey(dataTag.getTag());
    }

    public void add(TaggedData taggedData) {
        add(taggedData.getParsedTags(), taggedData.getHeight());
    }

    public void add(String[] tags, int height) {
        for (String tagValue : tags) {
            DataTag dataTag = get(tagDbKeyFactory.newKey(tagValue));
            if (dataTag == null) {
                if (log.isTraceEnabled()) {
                    log.trace("Add new tag value {} at height {} - {}", tagValue, height, last3Stacktrace());
                }
                dataTag = new DataTag(tagValue);
            }
            dataTag.setHeight(height);
            dataTag.setCount(dataTag.getCount() + 1);
            if (log.isTraceEnabled()) {
                log.trace("New quantity for tag value {} - {} at {} - ", tagValue, dataTag.getCount(), height, last3Stacktrace());
            }
            insert(dataTag);
        }
    }

    @Override
    public boolean isScanSafe() {
        return false; // data tag is tightly couple with tagged data so that also cannot be recovered from blockchain, so we should not rollback it during scan
    }

    public void add(TaggedData taggedData, int height) {
        if (log.isTraceEnabled()) {
            log.trace("Restore tagged data at {} - tags - {}  : {}", height, Arrays.toString(taggedData.getParsedTags()), last3Stacktrace());
        }
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
                if (log.isTraceEnabled()) {
                    log.trace("Reduced mapTag count for {} by {} - ", entry.getKey(), entry.getValue(), last3Stacktrace());
                }
            }
            int deleted = pstmtDelete.executeUpdate();
            if (deleted > 0) {
                log.trace("Deleted {} tags", deleted);
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
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement(
                "MERGE INTO data_tag (tag, tag_count, height, latest) "
                    + "KEY (tag, height) VALUES (?, ?, ?, TRUE)")
        ) {
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
