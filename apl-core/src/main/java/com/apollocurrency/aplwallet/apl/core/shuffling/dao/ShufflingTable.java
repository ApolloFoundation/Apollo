/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.shuffling.dao;

import com.apollocurrency.aplwallet.apl.core.app.Shuffling;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ShufflingTable  extends VersionedDeletableEntityDbTable<Shuffling> {
        private static final LongKeyFactory<Shuffling> KEY_FACTORY = new LongKeyFactory<Shuffling>("id") {

            @Override
            public DbKey newKey(Shuffling shuffling) {
                return shuffling.dbKey;
            }

        };
        private static final String TABLE_NAME = "shuffling";

        @Override
        public Shuffling load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new Shuffling(rs, dbKey);
        }



        @Override
        public void save(Connection con, Shuffling shuffling) throws SQLException {
            shuffling.save(con);
            LOG.trace("Save shuffling {} - height - {} remaining - {} Trace - {}", shuffling.getId(), shuffling.getHeight(), shuffling.getBlocksRemaining(),  shuffling.last3Stacktrace());
        }


}
