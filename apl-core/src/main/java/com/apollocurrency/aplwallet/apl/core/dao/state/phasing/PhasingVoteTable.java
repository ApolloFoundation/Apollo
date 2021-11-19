/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.phasing;

import com.apollocurrency.aplwallet.apl.core.converter.db.phasing.PhasingVoteMapper;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LinkKeyFactory;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextOperationData;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.entity.state.phasing.PhasingVote;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.utils.CollectionUtil;

import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Singleton
public class PhasingVoteTable extends EntityDbTable<PhasingVote> {
    static final LinkKeyFactory<PhasingVote> KEY_FACTORY = new LinkKeyFactory<PhasingVote>("transaction_id", "voter_id") {
        @Override
        public DbKey newKey(PhasingVote vote) {
            if (vote.getDbKey() == null) {
                vote.setDbKey(new LinkKey(vote.getPhasedTransactionId(), vote.getVoterId()));
            }
            return vote.getDbKey();
        }
    };
    private static final PhasingVoteMapper MAPPER = new PhasingVoteMapper(KEY_FACTORY);
    private static final String TABLE_NAME = "phasing_vote";

    @Inject
    public PhasingVoteTable(DatabaseManager databaseManager,
                            Event<FullTextOperationData> fullTextOperationDataEvent) {
        super(TABLE_NAME, KEY_FACTORY, false, null,
            databaseManager, fullTextOperationDataEvent);
    }

    @Override
    public PhasingVote load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return MAPPER.map(rs, null);
    }

    @Override
    public void save(Connection con, PhasingVote vote) throws SQLException {
        try (PreparedStatement pstmt = con.prepareStatement("INSERT INTO phasing_vote (vote_id, transaction_id, "
            + "voter_id, height) VALUES (?, ?, ?, ?)")) {
            int i = 0;
            pstmt.setLong(++i, vote.getVoteId());
            pstmt.setLong(++i, vote.getPhasedTransactionId());
            pstmt.setLong(++i, vote.getVoterId());
            pstmt.setInt(++i, vote.getHeight());
            pstmt.executeUpdate();
        }
    }

    public PhasingVote get(long phasedTransactionId, long voterId) {
        return get(KEY_FACTORY.newKey(phasedTransactionId, voterId));
    }

    public List<PhasingVote> get(long phasingTransactionId) {
        return CollectionUtil.toList(getManyBy(new DbClause.LongClause("transaction_id", phasingTransactionId), 0, -1));
    }
}
