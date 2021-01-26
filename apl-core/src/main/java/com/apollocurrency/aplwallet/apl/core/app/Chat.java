/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.dao.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionService;

import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes.TransactionTypeSpec.ARBITRARY_MESSAGE;

public class Chat {
    private static final TransactionService transactionService = CDI.current().select(TransactionService.class).get();
    private static DatabaseManager databaseManager;

    private static TransactionalDataSource lookupDataSource() {
        if (databaseManager == null) {
            databaseManager = CDI.current().select(DatabaseManager.class).get();
        }
        return databaseManager.getDataSource();
    }

    public static DbIterator<ChatInfo> getChatAccounts(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = lookupDataSource().getConnection();
            PreparedStatement stmt = con.prepareStatement(
                "select account, max(timestamp) as timestamp from "
                    + "((SELECT recipient_id as account, timestamp from transaction "
                    + "where type = ? and subtype = ? and sender_id =?) "
                    + "union "
                    + "(SELECT sender_id as account, timestamp from transaction "
                    + "where type = ? and subtype = ? and recipient_id = ?)) "
                    + "group by account order by timestamp desc "
                    + DbUtils.limitsClause(from, to)
            );
            int i = 0;
            stmt.setByte(++i, ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, accountId);
            stmt.setByte(++i, ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, stmt, from, to);
            return new DbIterator<>(con, stmt, (conection, rs) -> {
                long account = rs.getLong("account");
                long timestamp = rs.getLong("timestamp");
                return new ChatInfo(account, timestamp);
            });
        } catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static List<Transaction> getChatHistory(long account1, long account2, int from, int to) {
        return transactionService.getTransactionsChatHistory(account1, account2, from, to);
    }

    public static class ChatInfo {
        long account;
        long lastMessageTime;

        public ChatInfo(long account, long lastMessageTime) {
            this.account = account;
            this.lastMessageTime = lastMessageTime;
        }

        public ChatInfo() {
        }

        public long getAccount() {
            return account;
        }

        public long getLastMessageTime() {
            return lastMessageTime;
        }
    }

}
