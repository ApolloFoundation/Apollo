/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Verified transaction info, representing successful verification's count,
 * transaction id, verification status and current error message
 * @author Andrii Boiarskyi
 * @see TxsVerificationResult
 * @since 1.48.4
 */
@AllArgsConstructor
@Getter
public class VerifiedTransaction {
    private final long id;
    private final String error;
    private final int count;
    private final boolean verified;
}
