/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.respons;

import lombok.Data;

import java.math.BigInteger;

@Data
public class GetCumulativeDifficultyResponse extends BaseP2PResponse {
    private Long blockchainHeight;
    private BigInteger cumulativeDifficulty;

}
