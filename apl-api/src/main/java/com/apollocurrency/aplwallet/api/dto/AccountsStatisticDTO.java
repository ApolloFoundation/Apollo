/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

import com.apollocurrency.aplwallet.api.dto.account.AccountDTO;

import java.util.List;

public class AccountsStatisticDTO {
    private long totalSupply;
    private long totalAmountOnTopAccounts;
    private long numberOfTopAccounts;
    private int totalNumberOfAccounts;
    private List<AccountDTO> topHolders;

    public long getTotalSupply() {
        return totalSupply;
    }

    public void setTotalSupply(long totalSupply) {
        this.totalSupply = totalSupply;
    }

    public long getTotalAmountOnTopAccounts() {
        return totalAmountOnTopAccounts;
    }

    public void setTotalAmountOnTopAccounts(long totalAmountOnTopAccounts) {
        this.totalAmountOnTopAccounts = totalAmountOnTopAccounts;
    }

    public long getNumberOfTopAccounts() {
        return numberOfTopAccounts;
    }

    public void setNumberOfTopAccounts(long numberOfTopAccounts) {
        this.numberOfTopAccounts = numberOfTopAccounts;
    }

    public int getTotalNumberOfAccounts() {
        return totalNumberOfAccounts;
    }

    public void setTotalNumberOfAccounts(int totalNumberOfAccounts) {
        this.totalNumberOfAccounts = totalNumberOfAccounts;
    }

    public List<AccountDTO> getTopHolders() {
        return topHolders;
    }

    public void setTopHolders(List<AccountDTO> topHolders) {
        this.topHolders = topHolders;
    }
}
