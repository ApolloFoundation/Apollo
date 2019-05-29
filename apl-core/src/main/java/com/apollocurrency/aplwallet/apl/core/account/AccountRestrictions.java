/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.account;

import static com.apollocurrency.aplwallet.apl.core.transaction.AccountControl.SET_PHASING_ONLY;
import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.core.account.Account.ControlType;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public final class AccountRestrictions {
        private static final Logger LOG = getLogger(AccountRestrictions.class);



    static final LongKeyFactory<PhasingOnly> phasingControlDbKeyFactory = new LongKeyFactory<PhasingOnly>("account_id") {
        @Override
        public DbKey newKey(PhasingOnly rule) {
            return rule.dbKey;
        }
    };

    static final VersionedDeletableEntityDbTable<PhasingOnly> phasingControlTable = new VersionedDeletableEntityDbTable<PhasingOnly>("account_control_phasing", phasingControlDbKeyFactory) {

        @Override
        public PhasingOnly load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
            return new PhasingOnly(rs, dbKey);
        }

        @Override
        public void save(Connection con, PhasingOnly phasingOnly) throws SQLException {
            phasingOnly.save(con);
        }
    };

    public static void init() {
    }

    public static void checkTransaction(Transaction transaction) throws AplException.NotCurrentlyValidException {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        if (senderAccount == null) {
            throw new AplException.NotCurrentlyValidException("Account " + Long.toUnsignedString(transaction.getSenderId()) + " does not exist yet");
        }
        if (senderAccount.getControls().contains(Account.ControlType.PHASING_ONLY)) {
            PhasingOnly phasingOnly = PhasingOnly.get(transaction.getSenderId());
            phasingOnly.checkTransaction(transaction);
        }
    }

    public static boolean isBlockDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        return
                senderAccount.getControls().contains(ControlType.PHASING_ONLY)
                && PhasingOnly.get(transaction.getSenderId()).getMaxFees() != 0
                && transaction.getType() != SET_PHASING_ONLY
                && TransactionType.isDuplicate(SET_PHASING_ONLY,
                        Long.toUnsignedString(senderAccount.getId()), duplicates, true);
    }

}
