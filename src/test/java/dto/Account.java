/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.util.Convert;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

public class Account {
    private long balanceATM;
    private long forgedBalanceATM;
    private String accountRS;
    private long unconfirmedBalanceATM;
    private long account;
    private double percentage;

    public void setBalanceATM(long balanceATM) {
        this.balanceATM = balanceATM;
    }

    public void setForgedBalanceATM(long forgedBalanceATM) {
        this.forgedBalanceATM = forgedBalanceATM;
    }

    public void setAccountRS(String accountRS) {
        this.accountRS = accountRS;
    }

    public void setUnconfirmedBalanceATM(long unconfirmedBalanceATM) {
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
    }
    @JsonSetter
    public void setAccount(String account) {
        this.account = Convert.parseAccountId(account);
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

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

    public Account(long balanceATM, long forgedBalanceATM, String accountRS, long unconfirmedBalanceATM, String account, double percentage) {
        this(balanceATM, forgedBalanceATM, accountRS, unconfirmedBalanceATM, Long.parseUnsignedLong(account), percentage);
    }
    public Account(long balanceATM, long forgedBalanceATM, long unconfirmedBalanceATM, String account) {
        this(balanceATM, forgedBalanceATM, Convert.defaultRsAccount( Long.parseUnsignedLong(account)), unconfirmedBalanceATM, account, 0);
    }
    public Account(long balanceATM, long forgedBalanceATM, String accountRS, long unconfirmedBalanceATM, long account, double percentage) {
        this.balanceATM = balanceATM;
        this.forgedBalanceATM = forgedBalanceATM;
        this.accountRS = accountRS;
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
        this.percentage = percentage;
        this.account = account;
    }
    public Account(long balanceATM, long forgedBalanceATM, long unconfirmedBalanceATM, long account) {
        this(balanceATM, forgedBalanceATM, Convert.defaultRsAccount(account), unconfirmedBalanceATM, account, 0);
    }

    public Account() {
    }
}
