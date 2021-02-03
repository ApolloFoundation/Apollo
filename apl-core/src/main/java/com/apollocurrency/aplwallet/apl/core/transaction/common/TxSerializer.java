/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.io.Result;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface TxSerializer {

    void serialize(Transaction transaction, Result result);

}
