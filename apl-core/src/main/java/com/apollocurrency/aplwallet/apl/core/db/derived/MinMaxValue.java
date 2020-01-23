/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.derived;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MinMaxValue {
    private long min;
    private long max;
    private String column;
    private long count;
    private int height;
}
