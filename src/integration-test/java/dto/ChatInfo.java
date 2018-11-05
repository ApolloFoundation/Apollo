/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.fasterxml.jackson.annotation.JsonAlias;

public class ChatInfo {
    @JsonAlias("accountRS")
    private BasicAccount account;
    private long lastMessageTime;

    public ChatInfo(BasicAccount account, long lastMessageTime) {
        this.account = account;
        this.lastMessageTime = lastMessageTime;
    }
    public ChatInfo(String account, long lastMessageTime) {
        this(new BasicAccount(account), lastMessageTime);
    }
    public BasicAccount getAccount() {
            return account;
        }

        public long getLastMessageTime() {
            return lastMessageTime;
        }

        public ChatInfo() {
        }

    public void setAccount(String account) {
        this.account = new BasicAccount(account);
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
}
