/*
 * Copyright (c)  2018-2020. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.signature;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignatureParserFactory {

    private static final SignatureParser parserV1 = new SigData.Parser();
    private static final SignatureParser parserV2 = new MultiSigData.Parser();

    public static Optional<SignatureParser> selectParser(int transactionVersion) {
        switch (transactionVersion) {
            case 0:
            case 1:
                return Optional.of(parserV1);
            case 2:
                return Optional.of(parserV2);
            default:
                //throw new UnsupportedTransactionVersion("Unsupported transaction version: " + transactionVersion);
                return Optional.empty();
        }
    }

}
