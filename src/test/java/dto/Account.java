/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

public class Account {
    private long balanceATM;
    private long forgedBalanceATM;
    private String accountRS;
    private long unconfirmedBalanceATM;
    private String account;

    public long getBalanceATM() {
        return balanceATM;
    }

    public long getForgedBalanceATM() {
        return forgedBalanceATM;
    }

    public String getAccountRS() {
        return accountRS;
    }

    public long getUnconfirmedBalanceATM() {
        return unconfirmedBalanceATM;
    }

    public String getAccount() {
        return account;
    }

    public Account(long balanceATM, long forgedBalanceATM, String accountRS, long unconfirmedBalanceATM, String account) {
        this.balanceATM = balanceATM;
        this.forgedBalanceATM = forgedBalanceATM;
        this.accountRS = accountRS;
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
        this.account = account;
    }

    public Account() {
    }
}
