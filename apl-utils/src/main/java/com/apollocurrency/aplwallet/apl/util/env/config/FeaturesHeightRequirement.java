package com.apollocurrency.aplwallet.apl.util.env.config;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class FeaturesHeightRequirement {
    private Integer dexReopenPendingOrdersHeight;
    private Integer dexExpiredContractWithFinishedPhasingHeightAndStep3;
    private Integer transactionV2Height;
    private Integer failedTransactionsAcceptanceHeight;
    private Integer transactionV3Height;

    @JsonCreator
    public FeaturesHeightRequirement(@JsonProperty("dexReopenPendingOrdersHeight") Integer dexReopenPendingOrdersHeight,
                                     @JsonProperty("dexExpiredContractWithFinishedPhasingHeightAndStep3") Integer dexExpiredContractWithFinishedPhasingHeightAndStep3,
                                     @JsonProperty("transactionV2Height") Integer transactionV2Height,
                                     @JsonProperty("failedTransactionsAcceptanceHeight") Integer failedTransactionsAcceptanceHeight,
                                     @JsonProperty("transactionV3Height") Integer transactionV3Height
    ) {
        this.dexReopenPendingOrdersHeight = dexReopenPendingOrdersHeight;
        this.dexExpiredContractWithFinishedPhasingHeightAndStep3 = dexExpiredContractWithFinishedPhasingHeightAndStep3;
        this.transactionV2Height = transactionV2Height;
        this.failedTransactionsAcceptanceHeight = failedTransactionsAcceptanceHeight;
        int v2h = transactionV2Height != null ? transactionV2Height: -1;
        this.transactionV3Height = transactionV3Height;
        int v3h = transactionV3Height != null ? transactionV3Height: -1;
        if( v2h >= v3h && v3h>0 ){
            throw new IllegalArgumentException("TransactionV3 height less then TransactionV2 height.");
        }
        if( v2h<0 && v3h>0 ){
            throw new IllegalArgumentException("TransactionV2 height isn't set.");
        }
    }

    public FeaturesHeightRequirement copy() {
        return new FeaturesHeightRequirement(dexReopenPendingOrdersHeight, dexExpiredContractWithFinishedPhasingHeightAndStep3, transactionV2Height, failedTransactionsAcceptanceHeight, transactionV3Height);
    }
}
