/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

public abstract class AbstractEncryptedMessageAppendixValidator<T extends AbstractEncryptedMessageAppendix> extends AbstractAppendixValidator<T> {
    private final BlockchainConfig config;

    public AbstractEncryptedMessageAppendixValidator(BlockchainConfig config) {
        this.config = config;
    }

    @Override
    public void validateStateDependent(Transaction transaction, T appendix, int validationHeight) throws AplException.ValidationException {
    }

    @Override
    public void validateStateIndependent(Transaction transaction, T appendix, int validationHeight) throws AplException.ValidationException {
        if (appendix.getEncryptedDataLength() > config.getCurrentConfig().getMaxEncryptedMessageLength()) {
            throw new AplException.NotValidException("Max encrypted message length exceeded");
        }
        EncryptedData encryptedData = appendix.getEncryptedData();
        if (encryptedData != null) {
            if ((encryptedData.getNonce().length != 32 && encryptedData.getData().length > 0)
                || (encryptedData.getNonce().length != 0 && encryptedData.getData().length == 0)) {
                throw new AplException.NotValidException("Invalid nonce length " + encryptedData.getNonce().length);
            }
        }
        boolean compressed = appendix.isCompressed();
        byte version = appendix.getVersion();
        if ((version != 2 && !compressed) || (version == 2 && compressed)) {
            throw new AplException.NotValidException("Version mismatch - version " + version + ", isCompressed " + compressed);
        }
    }
}
