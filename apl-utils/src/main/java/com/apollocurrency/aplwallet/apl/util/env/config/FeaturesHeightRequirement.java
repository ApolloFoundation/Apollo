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

    @JsonCreator
    public FeaturesHeightRequirement(@JsonProperty("dexReopenPendingOrdersHeight") Integer dexReopenPendingOrdersHeight,
                                     @JsonProperty("dexExpiredContractWithFinishedPhasingHeightAndStep3") Integer dexExpiredContractWithFinishedPhasingHeightAndStep3
    ) {
        this.dexReopenPendingOrdersHeight = dexReopenPendingOrdersHeight;
        this.dexExpiredContractWithFinishedPhasingHeightAndStep3 = dexExpiredContractWithFinishedPhasingHeightAndStep3;
    }

    public FeaturesHeightRequirement copy(){
        return new FeaturesHeightRequirement(dexReopenPendingOrdersHeight, dexExpiredContractWithFinishedPhasingHeightAndStep3);
    }
}
