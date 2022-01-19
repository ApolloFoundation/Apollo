/*
 * Copyright (c)  2018-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.util.api.Range;
import com.apollocurrency.aplwallet.apl.util.api.Sort;
import lombok.Builder;
import lombok.Data;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder()
public class ContractQuery {
    Long address;
    Long transaction;
    Long owner;
    String name;
    String baseContract;
    Long timestamp;
    String status;
    int height;
    Sort order;
    Range paging;
}
