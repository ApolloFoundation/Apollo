/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.phasing.mapper.PhasingPollResultMapper;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;

import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Singleton
public class PhasingPollResultTable extends EntityDbTable<PhasingPollResult> {

    private static final String TABLE_NAME = "phasing_poll_result";
    private static final LongKeyFactory<PhasingPollResult> KEY_FACTORY = new LongKeyFactory<PhasingPollResult>("id") {
        @Override
        public DbKey newKey(PhasingPollResult phasingPollResult) {
            if (phasingPollResult.getDbKey() == null) {
                DbKey dbKey = KEY_FACTORY.newKey(phasingPollResult.getId());
                phasingPollResult.setDbKey(dbKey);
            }
            return phasingPollResult.getDbKey();
        }
    };
    private static final PhasingPollResultMapper MAPPER = new PhasingPollResultMapper(KEY_FACTORY);

    public PhasingPollResultTable() {
        super(TABLE_NAME, KEY_FACTORY, false);
    }


    @Override
    public PhasingPollResult load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    public PhasingPollResult get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    @Override
    public void save(Connection con, PhasingPollResult phasingPollResult) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_poll_result (id, "
            + "result, approved, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, phasingPollResult.getId());
            pstmt.setLong(++i, phasingPollResult.getResult());
            pstmt.setBoolean(++i, phasingPollResult.isApproved());
            pstmt.setInt(++i, phasingPollResult.getHeight());
            pstmt.executeUpdate();
        }
    }
}
