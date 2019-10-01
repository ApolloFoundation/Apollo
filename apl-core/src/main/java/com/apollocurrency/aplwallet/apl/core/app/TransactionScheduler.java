package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.util.Filter;
import lombok.Data;

@Data
class TransactionScheduler {
    private final Transaction transaction;
    private final Filter<Transaction> filter;
}
