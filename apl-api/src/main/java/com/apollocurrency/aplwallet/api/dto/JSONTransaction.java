/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.dto;

public class JSONTransaction { //implements Transaction {
    //   private Transaction transaction;
    private int numberOfConfirmations;

    public int getNumberOfConfirmations() {
        return numberOfConfirmations;
    }

    public void setNumberOfConfirmations(int numberOfConfirmations) {
        this.numberOfConfirmations = numberOfConfirmations;
    }
}
