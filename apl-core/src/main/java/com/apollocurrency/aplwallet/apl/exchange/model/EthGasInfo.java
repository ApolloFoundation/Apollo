package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class EthGasInfo {


    private Double fastSpeedPrice;

    private Double averageSpeedPrice;

    private Double safeLowSpeedPrice;



    public EthGasInfoDto toDto(){
        EthGasInfoDto ethGasInfoDto = new EthGasInfoDto();
        ethGasInfoDto.fast = this.fastSpeedPrice.toString();
        ethGasInfoDto.average = this.averageSpeedPrice.toString();
        ethGasInfoDto.safeLow = this.safeLowSpeedPrice.toString();

        return ethGasInfoDto;
    }

    public Double getFastSpeedPrice() {
        return fastSpeedPrice;
    }

    @JsonProperty("fast")
    public void setFastSpeedPrice(Double fastSpeedPrice) {
        this.fastSpeedPrice = fastSpeedPrice;
    }

    public Double getAverageSpeedPrice() {
        return averageSpeedPrice;
    }

    @JsonProperty("average")
    public void setAverageSpeedPrice(Double averageSpeedPrice) {
        this.averageSpeedPrice = averageSpeedPrice;
    }

    public Double getSafeLowSpeedPrice() {
        return safeLowSpeedPrice;
    }

    @JsonProperty("safeLow")
    public void setSafeLowSpeedPrice(Double safeLowSpeedPrice) {
        this.safeLowSpeedPrice = safeLowSpeedPrice;
    }
}
