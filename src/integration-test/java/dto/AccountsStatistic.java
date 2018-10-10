/*
 * Copyright © 2018 Apollo Foundation
 */

package dto;

import java.util.List;

public class AccountsStatistic {
    private long totalSupply;
    private long totalAmountOnTopAccounts;
    private long numberOfTopAccounts;
    private int totalNumberOfAccounts;
    private List<Account> topHolders;

    public void setTotalSupply(long totalSupply) {
        this.totalSupply = totalSupply;
    }

    public void setTotalAmountOnTopAccounts(long totalAmountOnTopAccounts) {
        this.totalAmountOnTopAccounts = totalAmountOnTopAccounts;
    }

    public void setNumberOfTopAccounts(long numberOfTopAccounts) {
        this.numberOfTopAccounts = numberOfTopAccounts;
    }

    public void setTotalNumberOfAccounts(int totalNumberOfAccounts) {
        this.totalNumberOfAccounts = totalNumberOfAccounts;
    }

    public long getTotalSupply() {
        return totalSupply;
    }

    public long getTotalAmountOnTopAccounts() {
        return totalAmountOnTopAccounts;
    }

    public long getNumberOfTopAccounts() {
        return numberOfTopAccounts;
    }

    public int getTotalNumberOfAccounts() {
        return totalNumberOfAccounts;
    }

    public List<Account> getTopHolders() {
        return topHolders;
    }

    public void setTopHolders(List<Account> topHolders) {
        this.topHolders = topHolders;
    }
}
