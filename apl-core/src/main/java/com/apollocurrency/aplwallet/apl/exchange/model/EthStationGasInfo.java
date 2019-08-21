package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EthStationGasInfo implements EthGasInfo {
    /**
     * Gwei
     */
    private Double fastSpeedPrice;
    /**
     * Gwei
     */
    private Double averageSpeedPrice;
    /**
     * Gwei
     */
    private Double safeLowSpeedPrice;

    public EthGasInfoDto toDto() {
        EthGasInfoDto ethGasInfoDto = new EthGasInfoDto();
        ethGasInfoDto.fast = this.fastSpeedPrice.toString();
        ethGasInfoDto.average = this.averageSpeedPrice.toString();
        ethGasInfoDto.safeLow = this.safeLowSpeedPrice.toString();

        return ethGasInfoDto;
    }

    public Long getFastSpeedPrice() {
        return fastSpeedPrice.longValue();
    }

    @JsonProperty("fast")
    public void setFastSpeedPrice(Double fastSpeedPrice) {
        this.fastSpeedPrice = fastSpeedPrice;
    }

    public Long getAverageSpeedPrice() {
        return averageSpeedPrice.longValue();
    }

    @JsonProperty("average")
    public void setAverageSpeedPrice(Double averageSpeedPrice) {
        this.averageSpeedPrice = averageSpeedPrice;
    }

    public Long getSafeLowSpeedPrice() {
        return safeLowSpeedPrice.longValue();
    }

    @JsonProperty("safeLow")
    public void setSafeLowSpeedPrice(Double safeLowSpeedPrice) {
        this.safeLowSpeedPrice = safeLowSpeedPrice;
    }
}
