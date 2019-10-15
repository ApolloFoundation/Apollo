package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.apl.util.Constants;

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
        if (ordinal < 0 || ordinal > OrderType.values().length) {
            return null;
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


    public Integer timeOfWaiting(){
        return this.isStep1() ? Constants.DEX_TIME_OF_WAITING_TX_WITH_APPROVAL_STEP_1 : Constants.DEX_TIME_OF_WAITING_TX_WITH_APPROVAL_STEP_2;
    }
}
