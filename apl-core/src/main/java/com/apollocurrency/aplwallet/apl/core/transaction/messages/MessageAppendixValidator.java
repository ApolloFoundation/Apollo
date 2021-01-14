/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageAppendixValidator extends AbstractAppendixValidator<MessageAppendix> {
    private final BlockchainConfig config;

    @Inject
    public MessageAppendixValidator(BlockchainConfig config) {
        this.config = config;
    }

    @Override
    public void validateStateDependent(Transaction transaction, MessageAppendix appendix, int validationHeight) throws AplException.ValidationException {
    }

    @Override
    public void validateStateIndependent(Transaction transaction, MessageAppendix appendix, int validationHeight) throws AplException.ValidationException {
        int length = appendix.getMessage().length;
        if (length > config.getCurrentConfig().getMaxArbitraryMessageLength()) {
            throw new AplException.NotValidException("Invalid arbitrary message length: " + length);
        }
    }

    @Override
    public Class<MessageAppendix> forClass() {
        return MessageAppendix.class;
    }
}
