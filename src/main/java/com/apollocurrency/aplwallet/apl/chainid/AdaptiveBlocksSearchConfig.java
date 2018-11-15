/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.chainid;

public class AdaptiveBlocksSearchConfig {
    private int startHeight;
    private int finishHeight;
    private int numberOfTransactions;

    public AdaptiveBlocksSearchConfig(int startHeight, int finishHeight, int numberOfTransactions) {
        this.startHeight = startHeight;
        this.finishHeight = finishHeight;
        this.numberOfTransactions = numberOfTransactions;
    }

    public int getStartHeight() {
        return startHeight;
    }

    public void setStartHeight(int startHeight) {
        this.startHeight = startHeight;
    }

    public int getFinishHeight() {
        return finishHeight;
    }

    public void setFinishHeight(int finishHeight) {
        this.finishHeight = finishHeight;
    }

    public int getNumberOfTransactions() {
        return numberOfTransactions;
    }

    public void setNumberOfTransactions(int numberOfTransactions) {
        this.numberOfTransactions = numberOfTransactions;
    }
}
