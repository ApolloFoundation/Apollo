/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.util.StringUtils;

/**
 * Indicates that requested feature is currently disabled on the node. Note that this exception is not the same
 * as {@link AplTransactionFeatureNotEnabledException} since it may be applicable only for non-transaction features
 * and does not participate in a consensus in any way
 * @author Andrii Boiarskyi
 * @see AplTransactionFeatureNotEnabledException
 * @see AplCoreLogicException
 * @since 1.48.4
 */
public class AplFeatureNotEnabledException extends AplCoreLogicException {

    public AplFeatureNotEnabledException(String feature, String additionalInfo) {
        super(createFeatureNotEnabledMessage(feature, additionalInfo));
    }
    public AplFeatureNotEnabledException(String feature) {
        this(feature, (String) null);
    }

    public AplFeatureNotEnabledException(String feature, Throwable cause) {
        this(feature, cause, null);
    }

    public AplFeatureNotEnabledException(String feature, Throwable cause, String additionalInfo) {
        super(createFeatureNotEnabledMessage(feature, additionalInfo), cause);
    }

    private static String createFeatureNotEnabledMessage(String feature, String additionalInfo) {
        return "Feature '" + feature + "' is not enabled" + (StringUtils.isNotBlank(additionalInfo) ? ", details: '" + additionalInfo + "'" : "");
    }
}
