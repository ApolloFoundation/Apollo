/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PrunablePlainMessageValidator extends AbstractAppendixValidator<PrunablePlainMessageAppendix> {
    private final TimeService timeService;
    private final BlockchainConfig blockchainConfig;

    @Inject
    public PrunablePlainMessageValidator(TimeService timeService, BlockchainConfig blockchainConfig) {
        this.timeService = timeService;
        this.blockchainConfig = blockchainConfig;
    }

    @Override
    public void validateStateDependent(Transaction transaction, PrunablePlainMessageAppendix appendix, int validationHeight) {
        // transaction can not be failed with a 'no-data' reason, so that such transactions should not be accepted at all
        if (appendix.getMessage() == null && timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMinPrunableLifetime()) {
            throw new AplUnacceptableTransactionValidationException("Message has been pruned prematurely", transaction);
        }
    }

    @Override
    public void validateStateIndependent(Transaction transaction, PrunablePlainMessageAppendix appendix, int validationHeight) throws AplException.ValidationException {
        if (transaction.getMessage() != null) {
            throw new AplException.NotValidException("Cannot have both message and prunable message attachments");
        }
        byte[] msg = appendix.getMessage();
        if (msg != null && msg.length > Constants.MAX_PRUNABLE_MESSAGE_LENGTH) {
            throw new AplException.NotValidException("Invalid prunable message length: " + msg.length);
        }
    }

    @Override
    public Class<PrunablePlainMessageAppendix> forClass() {
        return PrunablePlainMessageAppendix.class;
    }
}
