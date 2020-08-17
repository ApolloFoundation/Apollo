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

    @JsonCreator
    public FeaturesHeightRequirement(@JsonProperty("dexReopenPendingOrdersHeight") Integer dexReopenPendingOrdersHeight,
                                     @JsonProperty("dexExpiredContractWithFinishedPhasingHeightAndStep3") Integer dexExpiredContractWithFinishedPhasingHeightAndStep3,
                                     @JsonProperty("transactionV2Height") Integer transactionV2Height
    ) {
        this.dexReopenPendingOrdersHeight = dexReopenPendingOrdersHeight;
        this.dexExpiredContractWithFinishedPhasingHeightAndStep3 = dexExpiredContractWithFinishedPhasingHeightAndStep3;
        this.transactionV2Height = transactionV2Height;
    }

    public FeaturesHeightRequirement copy() {
        return new FeaturesHeightRequirement(dexReopenPendingOrdersHeight, dexExpiredContractWithFinishedPhasingHeightAndStep3, transactionV2Height);
    }
}
