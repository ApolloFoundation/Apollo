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
import java.util.List;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.BlockchainImpl;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.tagged.mapper.TaggedDataExtendDataMapper;
import com.apollocurrency.aplwallet.apl.core.tagged.model.TaggedDataExtend;

@Singleton
public class TaggedDataExtendDao extends VersionedDeletableValuesDbTable<TaggedDataExtend> {

    protected DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();
    private Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();

    private static final String DB_TABLE = "tagged_data_extend";
    private static final TaggedDataExtendDataMapper MAPPER = new TaggedDataExtendDataMapper();

    private static final LongKeyFactory<TaggedDataExtend> taggedDataKeyFactory = new LongKeyFactory<>("id") {
        @Override
        public DbKey newKey(TaggedDataExtend taggedData) {
            if (taggedData.getDbKey() == null) {
                taggedData.setDbKey(new LongKey(taggedData.getTaggedDataId()));
            }
            return taggedData.getDbKey();
        }
    };


    public TaggedDataExtendDao() {
        super(DB_TABLE, taggedDataKeyFactory);
    }

    public List<TaggedDataExtend> getExtendTransactionIds(long taggedDataId) {
        return this.get(taggedDataKeyFactory.newKey(taggedDataId));
    }

    public List<TaggedDataExtend> get(Long id){
        return get(taggedDataKeyFactory.newKey(id));
    }

    @Override
    public void save(Connection con, TaggedDataExtend taggedData) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement(
                "INSERT INTO tagged_data_extend (id, extend_id, "
                        + "height, latest) VALUES (?, ?, ?, TRUE)")) {
            int i = 0;
            pstmt.setLong(++i, taggedData.getTaggedDataId());
            pstmt.setLong(++i, taggedData.getExtendId());
            pstmt.setInt(++i, taggedData.getHeight());  // TODO: YL check and fix later
            pstmt.executeUpdate();
        }
    }

    @Override
    public TaggedDataExtend load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        TaggedDataExtend taggedDataExtend = MAPPER.map(rs, null);
        taggedDataExtend.setDbKey(dbKey);
        return taggedDataExtend;
    }

}
