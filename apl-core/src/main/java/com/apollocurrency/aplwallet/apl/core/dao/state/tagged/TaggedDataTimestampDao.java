/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.tagged;

import com.apollocurrency.aplwallet.apl.core.converter.db.tagged.TagDataTimestampMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataTimestamp;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DAO for TaggedDataTimestamp
 */
@Singleton
public class TaggedDataTimestampDao extends EntityDbTable<TaggedDataTimestamp> {

    private static final LongKeyFactory<TaggedDataTimestamp> timestampKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(TaggedDataTimestamp timestamp) {
            return new LongKey(timestamp.getId());
        }
    };

    private static final String TABLE_NAME = "tagged_data_timestamp";
    private final TagDataTimestampMapper MAPPER = new TagDataTimestampMapper();

    public TaggedDataTimestampDao() {
        super(TABLE_NAME, timestampKeyFactory, true, null, false);
    }

    public DbKey newDbKey(TaggedDataTimestamp dataTimestamp) {
        return timestampKeyFactory.newKey(dataTimestamp.getId());
    }

    @Override
    public TaggedDataTimestamp load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        TaggedDataTimestamp dataTimestamp = MAPPER.map(rs, null);
        dataTimestamp.setDbKey(dbKey);
        return dataTimestamp;
    }

    public void save(Connection con, TaggedDataTimestamp dataTimestamp) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            @DatabaseSpecificDml(DmlMarker.RESERVED_KEYWORD_USE)
            PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO tagged_data_timestamp (id, `timestamp`, height, latest) "
                    + "VALUES (?, ?, ?, TRUE) "
                    + "ON DUPLICATE KEY UPDATE "
                    + "id = VALUES(id) , `timestamp` = VALUES(`timestamp`), height = VALUES(height), latest = TRUE")
        ) {
            int i = 0;
            pstmt.setLong(++i, dataTimestamp.getId());
            pstmt.setInt(++i, dataTimestamp.getTimestamp());
            pstmt.setInt(++i, dataTimestamp.getHeight());
            pstmt.executeUpdate();
        }
    }
}