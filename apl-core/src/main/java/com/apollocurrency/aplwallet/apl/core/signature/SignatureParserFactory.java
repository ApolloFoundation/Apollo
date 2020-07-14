/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.UnsupportedTransactionVersion;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
@Singleton
public class SignatureParserFactory {

    private final SignatureParser parserV1;
    private final SignatureParser parserV2;
    private final TransactionVersionValidator versionValidator;

    @Inject
    public SignatureParserFactory(TransactionVersionValidator versionValidator) {
        this.versionValidator = Objects.requireNonNull(versionValidator);
        this.parserV1 = new SigData.Parser();
        this.parserV2 = new MultiSigData.Parser();
    }

    public SignatureParser createParser(int transactionVersion) {
        versionValidator.checkVersion(transactionVersion);

        switch (transactionVersion) {
            case 0:
            case 1:
                return parserV1;
            case 2:
                return parserV2;
            default:
                throw new UnsupportedTransactionVersion("Unsupported transaction version: " + transactionVersion);
        }
    }

}
