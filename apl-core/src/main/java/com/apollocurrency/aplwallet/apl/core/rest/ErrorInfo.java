/*
 *
 *  Copyright © 2018-2019 Apollo Foundation
 *
 */

package com.apollocurrency.aplwallet.apl.core.rest;

public interface ErrorInfo {
    int getErrorCode();

    int getOldErrorCode();

    String getErrorDescription();
}
