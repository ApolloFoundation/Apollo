/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MinMaxDbId {
    private long minDbId = -1L;
    private long maxDbId = -1L;
    private long count;
    private int height;
}
