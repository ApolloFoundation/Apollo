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
    private Integer dexExpiredContractWithFinishedPhasingHeight;

    @JsonCreator
    public FeaturesHeightRequirement(@JsonProperty("dexReopenPendingOrdersHeight") Integer dexReopenPendingOrdersHeight,
                                     @JsonProperty("dexExpiredContractWithFinishedPhasingHeight") Integer dexExpiredContractWithFinishedPhasingHeight
    ) {
        this.dexReopenPendingOrdersHeight = dexReopenPendingOrdersHeight;
        this.dexExpiredContractWithFinishedPhasingHeight = dexExpiredContractWithFinishedPhasingHeight;
    }

    public FeaturesHeightRequirement copy(){
        return new FeaturesHeightRequirement(dexReopenPendingOrdersHeight, dexExpiredContractWithFinishedPhasingHeight);
    }
}
