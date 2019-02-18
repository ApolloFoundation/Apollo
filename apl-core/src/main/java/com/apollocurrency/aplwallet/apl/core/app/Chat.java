/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.transaction.Messaging;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import javax.enterprise.inject.spi.CDI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;

public class Chat {
    private static Blockchain blockchain = CDI.current().select(BlockchainImpl.class).get();
    private static DatabaseManager databaseManager = CDI.current().select(DatabaseManager.class).get();

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
            stmt.setByte(++i, Messaging.ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, Messaging.ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, accountId);
            stmt.setByte(++i, Messaging.ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, Messaging.ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, accountId);
            DbUtils.setLimits(++i, stmt, from, to);
            return new DbIterator<>(con, stmt, (conection, rs) -> {
                long account = rs.getLong("account");
                long timestamp = rs.getLong("timestamp");
                return new ChatInfo(account, timestamp);
            });
        }
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }

    public static DbIterator<? extends Transaction> getChatHistory(long account1, long account2, int from, int to) {
        Connection con = null;
        try {
            con = lookupDataSource().getConnection();
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT * from transaction "
                            + "where type = ? and subtype = ? and ((sender_id =? and recipient_id = ?) or  (sender_id =? and recipient_id = ?)) " +
                            "order by timestamp desc"
                            + DbUtils.limitsClause(from, to)
            );
            int i = 0;
            stmt.setByte(++i, Messaging.ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, Messaging.ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, account1);
            stmt.setLong(++i, account2);
            stmt.setLong(++i, account2);
            stmt.setLong(++i, account1);
            DbUtils.setLimits(++i, stmt, from, to);
            return blockchain.getTransactions(con, stmt);
        }
        catch (SQLException e) {
            DbUtils.close(con);
            throw new RuntimeException(e.toString(), e);
        }
    }


    public static class ChatInfo {
        long account;
        long lastMessageTime;

        public ChatInfo(long account, long lastMessageTime) {
            this.account = account;
            this.lastMessageTime = lastMessageTime;
        }

        public long getAccount() {
            return account;
        }

        public long getLastMessageTime() {
            return lastMessageTime;
        }

        public ChatInfo() {
        }
    }

}
