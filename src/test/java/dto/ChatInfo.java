/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.util.Convert;

public class ChatInfo {
    private long account;
    private String accountRS;
    private long lastMessageTime;

    public ChatInfo(long account, String accountRS, long lastMessageTime) {
        this.account = account;
        this.accountRS = accountRS;
        this.lastMessageTime = lastMessageTime;
    }
    public ChatInfo(long account, long lastMessageTime) {
        this.account = account;
        this.accountRS = Convert.rsAccount(account);
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

    public void setAccount(long account) {
        this.account = account;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
