/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.util.exception;

public interface ApiErrorInfo {
    int getErrorCode();

    int getOldErrorCode();

    String getErrorDescription();
}
