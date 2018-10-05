/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;
import com.fasterxml.jackson.annotation.JsonCreator;

public class ChatInfo {
    private BasicAccount account;
    private long lastMessageTime;
    @JsonCreator
    public ChatInfo(String account, long lastMessageTime) {
        this.account = new BasicAccount(account);
        this.lastMessageTime = lastMessageTime;
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
