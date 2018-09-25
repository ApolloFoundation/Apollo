/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import java.util.Objects;

public class Account {
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private long balanceATM;
    private long forgedBalanceATM;
    private String accountRS;
    private long unconfirmedBalanceATM;
    private long account;
    private double percentage;

    @Override
    public String toString() {
        return "Account{" +
                "balanceATM=" + balanceATM +
                ", forgedBalanceATM=" + forgedBalanceATM +
                ", accountRS='" + accountRS + '\'' +
                ", unconfirmedBalanceATM=" + unconfirmedBalanceATM +
                ", account='" + account + '\'' +
                ", percentage=" + percentage +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        Account account1 = (Account) o;
        return balanceATM == account1.balanceATM &&
                forgedBalanceATM == account1.forgedBalanceATM &&
                unconfirmedBalanceATM == account1.unconfirmedBalanceATM &&
                Double.compare(account1.percentage, percentage) == 0 &&
                Objects.equals(accountRS, account1.accountRS) &&
                Objects.equals(account, account1.account);
    }

    @Override
    public int hashCode() {
        return Objects.hash(balanceATM, forgedBalanceATM, accountRS, unconfirmedBalanceATM, account, percentage);
    }

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

    public long getAccount() {
        return account;
    }

    public Account(long balanceATM, long forgedBalanceATM, String accountRS, long unconfirmedBalanceATM, long account, double percentage) {
        this.balanceATM = balanceATM;
        this.forgedBalanceATM = forgedBalanceATM;
        this.accountRS = accountRS;
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
        this.percentage = percentage;
        this.account = account;
    }

    public Account() {
    }
}
