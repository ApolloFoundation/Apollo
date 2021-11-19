/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigInteger;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Builder
@Data
public class BlockEntity {
    @EqualsAndHashCode.Exclude
    private long dbId;
    private long id;

    private int version;
    private int timestamp;
    private long previousBlockId;

    private long totalAmountATM;
    private long totalFeeATM;
    private int payloadLength;
    private byte[] previousBlockHash;
    private BigInteger cumulativeDifficulty;
    private long baseTarget;
    private long nextBlockId;
    private int height;
    private byte[] generationSignature;
    private byte[] blockSignature;
    private byte[] payloadHash;
    private long generatorId;
    private int timeout;
}
