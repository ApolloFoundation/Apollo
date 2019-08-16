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
    private Integer phasingApprovalTx;

    @JsonCreator
    public FeaturesHeightRequirement(@JsonProperty("phasingApprovalTx") Integer phasingApprovalTx) {
        this.phasingApprovalTx = phasingApprovalTx;
    }

    public FeaturesHeightRequirement copy(){
        return new FeaturesHeightRequirement(phasingApprovalTx);
    }
}
