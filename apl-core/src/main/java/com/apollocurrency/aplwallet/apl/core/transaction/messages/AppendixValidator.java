/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

public interface AppendixValidator<T extends Appendix> {

    /**
     * Should perform state-dependent validation of specific Appendix and throw {@link AplException.ValidationException} when validation is not successful
     * @param transaction transaction to validate
     * @param appendix transaction appendix to validate
     * @throws AplException.ValidationException when validation fails
     */
    void validateStateDependent(Transaction transaction, T appendix, int validationHeight) throws AplException.ValidationException;

    /**
     * Phasing transaction will be validated by this method instead of {@link AppendixValidator#validateStateDependent(Transaction, Appendix, int)} + {@link AppendixValidator#validateStateIndependent(Transaction, Appendix, int)}  when will be approved and executed
     * @param transaction transaction to validate
     * @param appendix transaction's appendix to validate
     * @param blockHeight blockchain height for validation
     * @throws AplException.ValidationException when validation fails
     */
    void validateAtFinish(Transaction transaction, T appendix, int blockHeight) throws AplException.ValidationException;


    /**
     * Perform stated-independent validation for appendix, this method should not use external resources, such as file
     * system, remote connections, sockets, any IO and so on. Validation should be lightweight and perform at max level
     * @param transaction transaction to validate
     * @param appendix transaction's appendix to validate
     * @param validationHeight blockchain height for validation
     * @throws AplException.ValidationException when validation fails
     */
    void validateStateIndependent(Transaction transaction, T appendix, int validationHeight) throws AplException.ValidationException;


    /**
     * @return class instance for which validation has to be performed
     */
    Class<T> forClass();
}
