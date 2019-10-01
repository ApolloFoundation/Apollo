package com.apollocurrency.aplwallet.apl.core.phasing.dao;


import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.phasing.mapper.PhasingApprovedResultMapper;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingApprovalResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class PhasingApprovedResultTable  extends EntityDbTable<PhasingApprovalResult> {

    private static final String TABLE_NAME = "phasing_approval_tx";
    private static final LongKeyFactory<PhasingApprovalResult> KEY_FACTORY = new LongKeyFactory<>("phasing_tx") {
        @Override
        public DbKey newKey(PhasingApprovalResult phasingApproveResult) {
            if (phasingApproveResult.getDbKey() == null) {
                DbKey dbKey = KEY_FACTORY.newKey(phasingApproveResult.getPhasingTx());
                phasingApproveResult.setDbKey(dbKey);
            }
            return phasingApproveResult.getDbKey();
        }
    };
    private static final PhasingApprovedResultMapper MAPPER = new PhasingApprovedResultMapper(KEY_FACTORY);

    public PhasingApprovedResultTable() {
        super(TABLE_NAME, KEY_FACTORY);
    }

    @Override
    public PhasingApprovalResult load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    public PhasingApprovalResult get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    @Override
    public void save(Connection con, PhasingApprovalResult phasingPollResult) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_approval_tx (phasing_tx, approved_tx, height) VALUES (?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, phasingPollResult.getPhasingTx());
            pstmt.setLong(++i, phasingPollResult.getApprovedTx());
            pstmt.setInt(++i, phasingPollResult.getHeight());
            pstmt.executeUpdate();
        }
    }
}
