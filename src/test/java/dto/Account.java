/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import com.apollocurrency.aplwallet.apl.BasicAccount;

import java.util.Objects;

public class Account extends BasicAccount {
    private long balanceATM;
    private long forgedBalanceATM;
    private long unconfirmedBalanceATM;
    private double percentage;

    public void setBalanceATM(long balanceATM) {
        this.balanceATM = balanceATM;
    }

    public void setForgedBalanceATM(long forgedBalanceATM) {
        this.forgedBalanceATM = forgedBalanceATM;
    }

    public void setUnconfirmedBalanceATM(long unconfirmedBalanceATM) {
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
    }
    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public String toString() {
        return "Account{" +
                "balanceATM=" + balanceATM +
                ", percentage=" + percentage +
//                ", account=" + account +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account)) return false;
        if (!super.equals(o)) return false;
        Account account = (Account) o;
        return balanceATM == account.balanceATM &&
                forgedBalanceATM == account.forgedBalanceATM &&
                unconfirmedBalanceATM == account.unconfirmedBalanceATM &&
                Double.compare(account.percentage, percentage) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), balanceATM, forgedBalanceATM, unconfirmedBalanceATM, percentage);
    }

    public long getBalanceATM() {
        return balanceATM;
    }

    public long getForgedBalanceATM() {
        return forgedBalanceATM;
    }

    public long getUnconfirmedBalanceATM() {
        return unconfirmedBalanceATM;
    }

    public Account(long balanceATM, long forgedBalanceATM, long unconfirmedBalanceATM, String account) {
        this(balanceATM, forgedBalanceATM, unconfirmedBalanceATM, account, 0);
    }
    public Account(long balanceATM, long forgedBalanceATM, long unconfirmedBalanceATM, String account, double percentage) {
        super(account);
        this.balanceATM = balanceATM;
        this.forgedBalanceATM = forgedBalanceATM;
        this.unconfirmedBalanceATM = unconfirmedBalanceATM;
        this.percentage = percentage;
    }
    public Account() {
    }
}
