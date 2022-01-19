/*
 * Copyright (c)  2018-2022. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.service.state.smc;

import com.apollocurrency.aplwallet.apl.util.api.Range;
import com.apollocurrency.aplwallet.apl.util.api.Sort;
import com.apollocurrency.smc.data.expr.Term;
import lombok.Builder;
import lombok.Data;

/**
 * @author andrew.zinchenko@gmail.com
 */
@Data
@Builder
public class ContractEventQuery {
    Long contract;
    String eventName;
    Term predicate;
    Range blockRange;
    Range paging;
    Sort order;
}
