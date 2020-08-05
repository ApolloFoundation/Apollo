/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.dgs;

import com.apollocurrency.aplwallet.apl.core.converter.db.dgs.DGSTagMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.StringKeyFactory;
import com.apollocurrency.aplwallet.apl.core.entity.state.dgs.DGSTag;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class DGSTagTable extends EntityDbTable<DGSTag> {
    public static final String TABLE_NAME = "tag";
    private static final StringKeyFactory<DGSTag> KEY_FACTORY = new StringKeyFactory<DGSTag>("tag") {
        @Override
        public DbKey newKey(DGSTag tag) {
            if (tag.getDbKey() == null) {
                tag.setDbKey(KEY_FACTORY.newKey(tag.getTag()));
            }
            return tag.getDbKey();
        }
    };
    private static final DGSTagMapper MAPPER = new DGSTagMapper(KEY_FACTORY);

    @Inject
    public DGSTagTable(DerivedTablesRegistry derivedDbTablesRegistry,
                       DatabaseManager databaseManager) {
        super(TABLE_NAME, KEY_FACTORY, true, null,
            derivedDbTablesRegistry, databaseManager, null);
    }

    @Override
    public DGSTag load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        DGSTag dgsTag = MAPPER.map(rs, null);
        dgsTag.setDbKey(dbKey);
        return dgsTag;
    }

    @Override
    public void save(Connection con, DGSTag tag) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("MERGE INTO tag (tag, in_stock_count, total_count, height, latest) "
                + "KEY (tag, height) VALUES (?, ?, ?, ?, TRUE)")
        ) {
            int i = 0;
            pstmt.setString(++i, tag.getTag());
            pstmt.setInt(++i, tag.getInStockCount());
            pstmt.setInt(++i, tag.getTotalCount());
            pstmt.setInt(++i, tag.getHeight());
            pstmt.executeUpdate();
        }
    }

    public DGSTag get(String tagValue) {
        return get(KEY_FACTORY.newKey(tagValue));
    }

    @Override
    public String defaultSort() {
        return " ORDER BY in_stock_count DESC, total_count DESC, tag ASC ";
    }
}
