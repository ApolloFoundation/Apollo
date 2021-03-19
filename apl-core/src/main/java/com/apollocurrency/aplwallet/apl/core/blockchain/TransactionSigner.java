/*
 * Copyright (c)  2020-2021. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.core.blockchain;

import com.apollocurrency.aplwallet.apl.core.signature.Credential;
import com.apollocurrency.aplwallet.apl.core.signature.DocumentSigner;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

/**
 * The transaction signer
 *
 * @author andrew.zinchenko@gmail.com
 * @deprecated Use the offline signing routine and KMS service
 */
@Deprecated
public interface TransactionSigner {
    void sign(Transaction transaction, byte[] keySeed) throws AplException.NotValidException;

    void sign(Transaction transaction, Credential credential);

    void sign(DocumentSigner documentSigner, Transaction transaction, Credential credential);

}
