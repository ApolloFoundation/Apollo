/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl;

import com.apollocurrency.aplwallet.apl.db.DbIterator;
import com.apollocurrency.aplwallet.apl.db.DbUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Chat {
    public static DbIterator<ChatInfo> getChatAccounts(long accountId, int from, int to) {
        Connection con = null;
        try {
            con = Db.db.getConnection();
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
            stmt.setByte(++i, TransactionType.Messaging.ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, TransactionType.Messaging.ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, accountId);
            stmt.setByte(++i, TransactionType.Messaging.ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, TransactionType.Messaging.ARBITRARY_MESSAGE.getSubtype());
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
            con = Db.db.getConnection();
            PreparedStatement stmt = con.prepareStatement(
                    "SELECT * from transaction "
                            + "where type = ? and subtype = ? and ((sender_id =? and recipient_id = ?) or  (sender_id =? and recipient_id = ?)) " +
                            "order by timestamp desc"
                            + DbUtils.limitsClause(from, to)
            );
            int i = 0;
            stmt.setByte(++i, TransactionType.Messaging.ARBITRARY_MESSAGE.getType());
            stmt.setByte(++i, TransactionType.Messaging.ARBITRARY_MESSAGE.getSubtype());
            stmt.setLong(++i, account1);
            stmt.setLong(++i, account2);
            stmt.setLong(++i, account2);
            stmt.setLong(++i, account1);
            DbUtils.setLimits(++i, stmt, from, to);
            return BlockchainImpl.getInstance().getTransactions(con, stmt);
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
