/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.p2p.respons;

import lombok.Data;

/**
 * P2P request to get failed transactions between heights with the limit set
 * @author Andrii Boiarskyi
 * @since 1.48.4
 */
@Data
public class GetFailedTransactionsRequest {
    private long fromHeight;
    private long toHeight;
    private int limit;
}
