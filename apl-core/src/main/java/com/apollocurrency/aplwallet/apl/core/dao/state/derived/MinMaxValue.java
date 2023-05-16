/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.dao.state.derived;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MinMaxValue {
    private BigDecimal min;
    private BigDecimal max;
    private String column;
    private long count;
    private int height;
}
