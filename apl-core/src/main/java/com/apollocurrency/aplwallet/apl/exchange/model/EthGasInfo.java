package com.apollocurrency.aplwallet.apl.exchange.model;

import com.apollocurrency.aplwallet.api.dto.EthGasInfoDto;

public interface EthGasInfo {

    Long getFastSpeedPrice();

    Long getAverageSpeedPrice();

    Long getSafeLowSpeedPrice();

    EthGasInfoDto toDto();

}
