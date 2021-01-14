package com.apollocurrency.aplwallet.apl.dex.eth.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class EthChainGasInfoImpl implements EthGasInfo {
    /**
     * wei
     */
    private Double fastSpeedPrice;
    /**
     * wei
     */
    private Double averageSpeedPrice;
    /**
     * wei
     */
    private Double safeLowSpeedPrice;


    public EthGasInfoDto toDto() {
        EthGasInfoDto ethGasInfoDto = new EthGasInfoDto();
        ethGasInfoDto.fast = this.getFastSpeedPrice().toString();
        ethGasInfoDto.average = this.getAverageSpeedPrice().toString();
        ethGasInfoDto.safeLow = this.getSafeLowSpeedPrice().toString();

        return ethGasInfoDto;
    }

    /**
     * Gwei
     */
    public Long getFastSpeedPrice() {
        return Double.valueOf(fastSpeedPrice).longValue();
    }

    @JsonProperty("fast")
    public void setFastSpeedPrice(Double fastSpeedPrice) {
        this.fastSpeedPrice = fastSpeedPrice;
    }

    /**
     * Gwei
     */
    public Long getAverageSpeedPrice() {
        return Double.valueOf(averageSpeedPrice).longValue();
    }

    @JsonProperty("standard")
    public void setAverageSpeedPrice(Double averageSpeedPrice) {
        this.averageSpeedPrice = averageSpeedPrice;
    }

    /**
     * Gwei
     */
    public Long getSafeLowSpeedPrice() {
        return Double.valueOf(safeLowSpeedPrice).longValue();
    }

    @JsonProperty("safeLow")
    public void setSafeLowSpeedPrice(Double safeLowSpeedPrice) {
        this.safeLowSpeedPrice = safeLowSpeedPrice;
    }
}
