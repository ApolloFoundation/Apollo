package com.apollocurrency.aplwallet.apl.exchange.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Data
@Builder
public class SwapDataInfo {
    private Long timeStart;
    private Long timeDeadLine;
    private byte[] secretHash;
    private byte[] secret;
    private Integer status;
    private String address;
    private String address2;

    //Wei
    private BigInteger amount;

}
