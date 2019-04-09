/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dgs.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.StringKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dgs.mapper.DGSTagMapper;
import com.apollocurrency.aplwallet.apl.core.dgs.model.DGSTag;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class DGSTagTable extends VersionedEntityDbTable<DGSTag> {
    private static final StringKeyFactory<DGSTag> KEY_FACTORY = new StringKeyFactory<DGSTag>("tag") {
        @Override
        public DbKey newKey(DGSTag tag) {
            if (tag.getDbKey() == null) {
                tag.setDbKey(KEY_FACTORY.newKey(tag.getTag()));
            }
            return tag.getDbKey();
        }
    };
    private static final DGSTagMapper MAPPER = new DGSTagMapper();
    public static final String TABLE_NAME = "tag";


    public DGSTagTable() {
        super(TABLE_NAME, KEY_FACTORY);
    }

    @Override
    public DGSTag load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        DGSTag dgsTag = MAPPER.map(rs, null);
        dgsTag.setDbKey(dbKey);
        return dgsTag;
    }

    @Override
    public void save(Connection con, DGSTag tag) throws SQLException {
        tag.save(con);
    }

    @Override
    public String defaultSort() {
        return " ORDER BY in_stock_count DESC, total_count DESC, tag ASC ";
    }
}
