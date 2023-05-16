package com.apollocurrency.aplwallet.apl.dex.eth.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;
import com.apollocurrency.aplwallet.apl.dex.eth.utils.EthUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class EthChainGasInfoImpl implements EthGasInfo {
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


    public EthGasInfoDto toDto() {
        EthGasInfoDto ethGasInfoDto = new EthGasInfoDto();
        ethGasInfoDto.fast = this.getFastSpeedPrice().toString();
        ethGasInfoDto.average = this.getAverageSpeedPrice().toString();
        ethGasInfoDto.safeLow = this.getSafeLowSpeedPrice().toString();

        return ethGasInfoDto;
    }

    @JsonProperty("data")
    public void setData(Map<String, Object> data) {
        // received data in wei
        this.fastSpeedPrice = EthUtil.weiToGwei(BigDecimal.valueOf((Long) data.get("fast"))).toBigInteger().longValueExact();
        this.averageSpeedPrice = EthUtil.weiToGwei(BigDecimal.valueOf((Long) data.get("standard"))).toBigInteger().longValueExact();
        this.safeLowSpeedPrice = EthUtil.weiToGwei(BigDecimal.valueOf((Long) data.get("slow"))).toBigInteger().longValueExact();
    }
}
