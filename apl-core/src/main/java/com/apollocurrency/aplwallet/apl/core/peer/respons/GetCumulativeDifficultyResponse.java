/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.peer.respons;

import lombok.Data;

import java.math.BigInteger;

@Data
public class GetCumulativeDifficultyResponse implements PeerResponse {
    private Long blockchainHeight;
    private BigInteger cumulativeDifficulty;

}
