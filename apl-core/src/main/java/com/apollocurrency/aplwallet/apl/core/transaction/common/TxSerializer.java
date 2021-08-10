/*
 * Copyright (c)  2018-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.transaction.common;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.util.io.Result;

/**
 * @author andrew.zinchenko@gmail.com
 */
public interface TxSerializer {

    void serialize(Transaction transaction, Result result);

}
