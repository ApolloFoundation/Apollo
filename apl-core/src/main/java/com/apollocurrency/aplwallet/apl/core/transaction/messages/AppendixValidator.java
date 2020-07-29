/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;

public interface AppendixValidator<T extends Appendix> {

    /**
     * Should validate specific Appendix and throw {@link AplException.ValidationException} when validation is not successful
     * @param transaction transaction to validate
     * @param appendix transaction appendix to validate
     * @throws AplException.ValidationException when validation fails
     */
    void validate(Transaction transaction, T appendix, int validationHeight) throws AplException.ValidationException;

    /**
     * Phasing transaction will be validated by this method instead of {@link AppendixValidator#validate(Transaction, Appendix, int)}
     * @param transaction transaction to validate
     * @param appendix transaction's appendix to validate
     * @param blockHeight blockchain height for validation
     * @throws AplException.ValidationException when validation fails
     */
    void validateAtFinish(Transaction transaction, T appendix, int blockHeight) throws AplException.ValidationException;
}
