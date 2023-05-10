/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.blockchain;

import com.apollocurrency.aplwallet.apl.core.exception.AplFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionNotFoundException;
import com.apollocurrency.aplwallet.apl.core.model.TxsVerificationResult;

import java.util.Optional;

/**
 * Service for failed transactions verification and status querying
 * @author Andrii Boiarskyi
 * @see TxsVerificationResult
 * @since 1.48.4
 */

public interface FailedTransactionVerificationService {
    /**
     * @return last failed transactions verification result if any
     */
    Optional<TxsVerificationResult> getLastVerificationResult();

    /**
     * Verifies failed transaction by the given id
     * @param id transaction id to verify
     * @return empty {@link TxsVerificationResult} when transaction is not failed or filled with verification results
     * @throws AplFeatureNotEnabledException when failed transaction acceptance is not enabled or transaction verification was disabled by node config
     * @throws AplTransactionNotFoundException when transaction by given id was not found in a blockchain
     */
    TxsVerificationResult verifyTransaction(long id);

    /**
     * @return last height on which failed transactions verification was done
     */
    int getLastVerifiedBlockHeight();

    /**
     * Launch batch failed transactions verification
     * @return verification result
     */
    Optional<TxsVerificationResult> verifyTransactions();
}
