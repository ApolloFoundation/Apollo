package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EthGasInfo {
    /**
     * Gwei
     */
    private Long fastSpeedPrice;
    /**
     * Gwei
     */
    private Long averageSpeedPrice;
    /**
     * Gwei
     */
    private Long safeLowSpeedPrice;



    public EthGasInfoDto toDto(){
        EthGasInfoDto ethGasInfoDto = new EthGasInfoDto();
        ethGasInfoDto.fast = this.fastSpeedPrice.toString();
        ethGasInfoDto.average = this.averageSpeedPrice.toString();
        ethGasInfoDto.safeLow = this.safeLowSpeedPrice.toString();

        return ethGasInfoDto;
    }

    public Long getFastSpeedPrice() {
        return fastSpeedPrice;
    }

    @JsonProperty("fast")
    public void setFastSpeedPrice(Long fastSpeedPrice) {
        this.fastSpeedPrice = fastSpeedPrice;
    }

    public Long getAverageSpeedPrice() {
        return averageSpeedPrice;
    }

    @JsonProperty("average")
    public void setAverageSpeedPrice(Long averageSpeedPrice) {
        this.averageSpeedPrice = averageSpeedPrice;
    }

    public Long getSafeLowSpeedPrice() {
        return safeLowSpeedPrice;
    }

    @JsonProperty("safeLow")
    public void setSafeLowSpeedPrice(Long safeLowSpeedPrice) {
        this.safeLowSpeedPrice = safeLowSpeedPrice;
    }
}
