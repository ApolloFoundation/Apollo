package com.apollocurrency.aplwallet.apl.util.io;

import java.io.IOException;

public class LimitExceededException extends IOException {
    public LimitExceededException(String message) {
        super(message);
    }
}
