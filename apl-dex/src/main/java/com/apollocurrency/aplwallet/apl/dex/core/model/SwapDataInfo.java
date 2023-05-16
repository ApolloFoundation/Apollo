package com.apollocurrency.aplwallet.apl.dex.core.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SwapDataInfo {
    private Long timeStart;
    private Long timeDeadLine;
    private byte[] secretHash;
    private byte[] secret;

    private String addressFrom;
    private String addressTo;
    private String addressAsset;

    //Eth
    private BigDecimal amount;
    private Integer status;

}
