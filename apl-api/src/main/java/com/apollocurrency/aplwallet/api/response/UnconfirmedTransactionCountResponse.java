/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.api.response;

import lombok.Data;

@Data
public class UnconfirmedTransactionCountResponse {
    private final int processed;
    private final int waiting;
}


