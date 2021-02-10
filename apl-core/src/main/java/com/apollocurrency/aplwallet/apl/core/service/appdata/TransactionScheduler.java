/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.Filter;
import lombok.Data;

@Data
public class TransactionScheduler {
    private final Transaction transaction;
    private final Filter<Transaction> filter;
}
