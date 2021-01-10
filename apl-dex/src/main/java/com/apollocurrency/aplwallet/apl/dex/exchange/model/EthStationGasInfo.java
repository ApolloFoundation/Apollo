/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.dex.exchange.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
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
        ethGasInfoDto.fast = this.getFastSpeedPrice().toString();
        ethGasInfoDto.average = this.getAverageSpeedPrice().toString();
        ethGasInfoDto.safeLow = this.getSafeLowSpeedPrice().toString();

        return ethGasInfoDto;
    }

    public Long getFastSpeedPrice() {
        return fastSpeedPrice.longValue();
    }

    @JsonProperty("fast")
    public void setFastSpeedPrice(Double fastSpeedPrice) {
        this.fastSpeedPrice = fastSpeedPrice / 10;
    }

    public Long getAverageSpeedPrice() {
        return averageSpeedPrice.longValue();
    }

    @JsonProperty("average")
    public void setAverageSpeedPrice(Double averageSpeedPrice) {
        this.averageSpeedPrice = averageSpeedPrice / 10;
    }

    public Long getSafeLowSpeedPrice() {
        return safeLowSpeedPrice.longValue();
    }

    @JsonProperty("safeLow")
    public void setSafeLowSpeedPrice(Double safeLowSpeedPrice) {
        this.safeLowSpeedPrice = safeLowSpeedPrice / 10;
    }
}
