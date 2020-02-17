package com.apollocurrency.aplwallet.apl.exchange.model;

public enum ExchangeContractStatus {
    /**
     * The first user sent a contract and he is waiting for review and approval. (On this step user doesn't transfer money.)
     */
    STEP_1,     // 0
    /**
     * The second user approved the contract, transferred the money and waiting for counter transfer.
     */
    STEP_2,     // 1
    /**
     * The first user sent counter transfer. (Process of the atomic swap is started.)
     */
    STEP_3,     // 2
    /**
     * Closed.
     */
    STEP_4;     // 3

    public static ExchangeContractStatus getType(int ordinal){
        if (ordinal < 0 || ordinal > ExchangeContractStatus.values().length) {
            throw new IllegalArgumentException("Contract status does not exist for ordinal " + ordinal);
        }
        return ExchangeContractStatus.values()[ordinal];
    }

    public boolean isStep1(){
        return this == ExchangeContractStatus.STEP_1;
    }

    public boolean isStep2(){
        return this == ExchangeContractStatus.STEP_2;
    }

    public boolean isStep3() {
        return this == ExchangeContractStatus.STEP_3;
    }

    public boolean isStep4() {
        return this == ExchangeContractStatus.STEP_4;
    }
}
