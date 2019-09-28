package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonProperty("fast")
    public void setFastSpeedPrice(Double fastSpeedPrice) {
        this.fastSpeedPrice = fastSpeedPrice;
    }

    @JsonProperty("standard")
    public void setAverageSpeedPrice(Double averageSpeedPrice) {
        this.averageSpeedPrice = averageSpeedPrice;
    }

    @JsonProperty("safeLow")
    public void setSafeLowSpeedPrice(Double safeLowSpeedPrice) {
        this.safeLowSpeedPrice = safeLowSpeedPrice;
    }

    /**
     * Gwei
     */
    public Long getFastSpeedPrice() {
        return Double.valueOf(fastSpeedPrice * 10).longValue();
    }

    /**
     * Gwei
     */
    public Long getAverageSpeedPrice() {
        return Double.valueOf(averageSpeedPrice * 10).longValue();
    }

    /**
     * Gwei
     */
    public Long getSafeLowSpeedPrice() {
        return Double.valueOf(safeLowSpeedPrice * 10).longValue();
    }
}
