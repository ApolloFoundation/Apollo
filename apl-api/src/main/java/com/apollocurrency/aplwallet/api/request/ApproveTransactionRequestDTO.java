package com.apollocurrency.aplwallet.api.request;

import java.util.Arrays;

public class ApproveTransactionRequestDTO {
    public String[] transactionFullHash;
    public String revealedSecret;
    public boolean revealedSecretIsText;
    public String revealedSecretText;

    @Override
    public String toString() {
        return "ApproveTransactionRequestDTO{" +
                "transactionFullHash=" + Arrays.toString(transactionFullHash) +
                ", revealedSecret='" + revealedSecret + '\'' +
                ", revealedSecretIsText=" + revealedSecretIsText +
                ", revealedSecretText='" + revealedSecretText + '\'' +
                '}';
    }
}
