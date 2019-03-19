/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import com.apollocurrency.aplwallet.apl.core.app.PhasingPollResult;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.EntityDbTable;
import com.apollocurrency.aplwallet.apl.core.db.KeyFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ResultTable extends EntityDbTable<PhasingPollResult> {

    private static final String TABLE_NAME = "phasing_poll_result";
    static final String KEY_FACTORY_NAME = TABLE_NAME + "_key_factory";

    @Inject
    public ResultTable(@Named(KEY_FACTORY_NAME) KeyFactory<PhasingPollResult> dbKeyFactory) {
        super(TABLE_NAME, resultDbKeyFactory));
    }

    @Override
    protected PhasingPollResult load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new PhasingPollResult(rs, dbKey);
    }

    @Override
    protected void save(Connection con, PhasingPollResult phasingPollResult) throws SQLException {
        phasingPollResult.save(con);
    }
};
