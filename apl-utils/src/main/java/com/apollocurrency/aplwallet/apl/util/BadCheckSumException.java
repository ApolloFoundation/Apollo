/*
 * Copyright © 2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util;

import java.io.IOException;

/**
 * @author al
 */
public class BadCheckSumException extends IOException {

    public BadCheckSumException(String message) {
        super(message);
    }

}
