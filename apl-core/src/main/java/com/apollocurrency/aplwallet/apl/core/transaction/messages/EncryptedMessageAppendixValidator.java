/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EncryptedMessageAppendixValidator extends AbstractEncryptedMessageAppendixValidator<EncryptedMessageAppendix> {
    @Inject
    public EncryptedMessageAppendixValidator(BlockchainConfig config) {
        super(config);
    }
}
