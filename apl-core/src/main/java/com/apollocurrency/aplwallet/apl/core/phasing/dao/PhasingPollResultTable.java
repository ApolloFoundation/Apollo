/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.phasing.dao;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.LongKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.phasing.model.PhasingPollResult;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Singleton;

@Singleton
public class PhasingPollResultTable extends EntityDbTable<PhasingPollResult> {

    private static final String TABLE_NAME = "phasing_poll_result";
    private static final LongKeyFactory<PhasingPollResult> KEY_FACTORY = new LongKeyFactory<PhasingPollResult>("id") {
        @Override
        public DbKey newKey(PhasingPollResult phasingPollResult) {
            return new LongKey(phasingPollResult.getId());
        }
    };

    public PhasingPollResultTable() {
        super(TABLE_NAME, KEY_FACTORY);
    }



    @Override
    protected PhasingPollResult load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new PhasingPollResult(rs);
    }

    public PhasingPollResult get(long id) {
        return get(KEY_FACTORY.newKey(id));
    }

    @Override
    protected void save(Connection con, PhasingPollResult phasingPollResult) throws SQLException {
        phasingPollResult.save(con);
    }
};
