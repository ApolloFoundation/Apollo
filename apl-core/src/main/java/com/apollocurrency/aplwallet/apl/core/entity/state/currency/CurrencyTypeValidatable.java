/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.entity.state.currency;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import java.util.Set;

public interface CurrencyTypeValidatable {

    default void validate(Currency currency, Transaction transaction,
                          Set<CurrencyType> validators, long maxBalanceAtm, boolean isActiveCurrency, int finishValidationHeight) throws AplException.ValidationException {
        // do nothing, all is valid
    }

    default void validateMissing(Currency currency, Transaction transaction,
                                 Set<CurrencyType> validators) throws AplException.ValidationException {
        // do nothing, all is valid
    }

}
