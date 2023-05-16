/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.crypto.EncryptedData;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class PrunableEncryptedMessageAppendixValidator extends AbstractAppendixValidator<PrunableEncryptedMessageAppendix> {
    private final TimeService timeService;
    private final BlockchainConfig blockchainConfig;

    @Inject
    public PrunableEncryptedMessageAppendixValidator(TimeService timeService, BlockchainConfig blockchainConfig) {
        this.timeService = timeService;
        this.blockchainConfig = blockchainConfig;
    }

    @Override
    public void validateStateDependent(Transaction transaction, PrunableEncryptedMessageAppendix appendix, int validationHeight) throws AplException.NotCurrentlyValidException {
        EncryptedData ed = appendix.getEncryptedData();
        // validate here at the end of validation cycle to ensure, that transaction is not failed and data
        // should be present for at least a minimum prunable lifetime
        // transaction can not be failed by 'no data' reason and should be not accepted at all
        validateDataExistence(transaction, ed);
    }

    @Override
    public void validateStateIndependent(Transaction transaction, PrunableEncryptedMessageAppendix appendix, int validationHeight) throws AplException.ValidationException {
        if (transaction.getEncryptedMessage() != null) {
            throw new AplException.NotValidException("Cannot have both encrypted and prunable encrypted message attachments");
        }
        EncryptedData ed = appendix.getEncryptedData();
        if (ed != null) {
            if (ed.getData().length > Constants.MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH) {
                throw new AplException.NotValidException(String.format("Message length %d exceeds max prunable encrypted message length %d",
                    ed.getData().length, Constants.MAX_PRUNABLE_ENCRYPTED_MESSAGE_LENGTH));
            }
            if ((ed.getNonce().length != 32 && ed.getData().length > 0)
                || (ed.getNonce().length != 0 && ed.getData().length == 0)) {
                throw new AplException.NotValidException("Invalid nonce length " + ed.getNonce().length);
            }
        }
        if (transaction.getRecipientId() == 0) {
            throw new AplException.NotValidException("Encrypted messages cannot be attached to transactions with no recipient");
        }
    }

    private void validateDataExistence(Transaction transaction, EncryptedData ed) {
        if (ed == null && timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMinPrunableLifetime()) {
            throw new AplUnacceptableTransactionValidationException("Encrypted message has been pruned prematurely", transaction);
        }
    }

    @Override
    public Class<PrunableEncryptedMessageAppendix> forClass() {
        return PrunableEncryptedMessageAppendix.class;
    }
}
