/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.tagged;

import com.apollocurrency.aplwallet.apl.core.converter.db.tagged.TaggedDataExtendDataMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.ValuesDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.tagged.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.core.shard.observer.DeleteOnTrimData;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Singleton
public class TaggedDataExtendDao extends ValuesDbTable<TaggedDataExtend> {

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
    protected DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();

    @Inject
    public TaggedDataExtendDao(DerivedTablesRegistry derivedDbTablesRegistry,
                               DatabaseManager databaseManager,
                               Event<DeleteOnTrimData> deleteOnTrimDataEvent) {
        super(DB_TABLE, taggedDataKeyFactory, true, derivedDbTablesRegistry, databaseManager, null, deleteOnTrimDataEvent);
    }

    public List<TaggedDataExtend> getExtendTransactionIds(long taggedDataId) {
        return this.get(taggedDataKeyFactory.newKey(taggedDataId));
    }

    public List<TaggedDataExtend> get(Long id) {
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
