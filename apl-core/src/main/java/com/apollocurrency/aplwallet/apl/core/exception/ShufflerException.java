/*
 * Copyright © 2020-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.exception;

import com.apollocurrency.aplwallet.apl.util.exception.AplException;

public class ShufflerException extends AplException {

    public ShufflerException(String message) {
        super(message);
    }

    public ShufflerException(String message, Throwable cause) {
        super(message, cause);
    }


    public static final class ShufflerLimitException extends ShufflerException {

        public ShufflerLimitException(String message) {
            super(message);
        }

    }

    public static final class DuplicateShufflerException extends ShufflerException {

        public DuplicateShufflerException(String message) {
            super(message);
        }

    }

    public static final class InvalidRecipientException extends ShufflerException {

        public InvalidRecipientException(String message) {
            super(message);
        }

    }

    public static final class ControlledAccountException extends ShufflerException {

        public ControlledAccountException(String message) {
            super(message);
        }

    }

    public static final class InvalidStageException extends ShufflerException {

        public InvalidStageException(String message) {
            super(message);
        }

    }


}
