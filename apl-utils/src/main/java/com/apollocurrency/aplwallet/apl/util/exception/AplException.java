/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.exception;

import com.apollocurrency.aplwallet.apl.util.JSON;
import org.json.simple.JSONStreamAware;

import java.io.IOException;
/**
 * TODO
 * Should be replaced by the {@code AplCoreLogicException} runtime exception's hierarchy
 */
@Deprecated(forRemoval = true)
public abstract class AplException extends Exception {

    private JSONStreamAware jsonResponse;

    protected AplException() {
        super();
    }

    protected AplException(String message) {
        super(message);
    }

    protected AplException(JSONStreamAware json) {
        super(JSON.toString(json));
        this.jsonResponse = json;
    }

    protected AplException(String message, Throwable cause) {
        super(message, cause);
    }

    protected AplException(Throwable cause) {
        super(cause);
    }

    public JSONStreamAware getJsonResponse() {
        return jsonResponse;
    }


    /**
     * Should be replaced by the {@code AplTransactionValidationException}
     */
    @Deprecated(forRemoval = true)
    public static abstract class ValidationException extends AplException {

        private ValidationException(String message) {
            super(message);
        }

        private ValidationException(JSONStreamAware json) {
            super(json);
        }

        private ValidationException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * Should be replaced by the {@code AplAcceptableTransactionValidationException} in most cases
     */
    @Deprecated(forRemoval = true)
    public static class NotCurrentlyValidException extends ValidationException {

        public NotCurrentlyValidException(String message) {
            super(message);
        }

        public NotCurrentlyValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * Should be replaced by the {@code AplUnacceptableTransactionValidationException} in most cases
     */
    @Deprecated(forRemoval = true)
    public static final class NotValidException extends ValidationException {

        public NotValidException(String message) {
            super(message);
        }

        public NotValidException(JSONStreamAware json) {
            super(json);
        }

        public NotValidException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * Should be replaced by the {@code AplAcceptableTransactionValidationException} subclass
     */
    @Deprecated(forRemoval = true)
    public static class AccountControlException extends NotCurrentlyValidException {

        public AccountControlException(String message) {
            super(message);
        }

        public AccountControlException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * Should be replaced by the {@code AplCoreLogicException} or separate P2P exception's hierarchy
     */
    @Deprecated(forRemoval = true)
    public static final class AplIOException extends IOException {

        public AplIOException(String message) {
            super(message);
        }

        public AplIOException(String message, Throwable cause) {
            super(message, cause);
        }

    }

    /**
     * TODO
     * Better to replaced by the subclass of the AplCoreLogicException
     */
    @Deprecated
    public static final class ExecutiveProcessException extends Exception {
        public ExecutiveProcessException() {
            super();
        }

        public ExecutiveProcessException(String message) {
            super(message);
        }

        public ExecutiveProcessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * TODO
     * Will be extracted out of this class
     */
    public static final class ThirdServiceIsNotAvailable extends RuntimeException {

        public ThirdServiceIsNotAvailable(String message) {
            super(message);
        }

        public ThirdServiceIsNotAvailable(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * TODO
     * Will be extracted out of this class
     */
    public static final class DEXProcessingException extends RuntimeException {

        public DEXProcessingException(String message) {
            super(message);
        }

        public DEXProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}
