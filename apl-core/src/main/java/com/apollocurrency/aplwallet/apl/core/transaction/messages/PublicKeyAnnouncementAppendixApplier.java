/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PublicKeyAnnouncementAppendixApplier implements AppendixApplier<PublicKeyAnnouncementAppendix>{
    private final AccountPublicKeyService accountPublicKeyService;

    @Inject
    public PublicKeyAnnouncementAppendixApplier(AccountPublicKeyService accountPublicKeyService) {
        this.accountPublicKeyService = accountPublicKeyService;
    }

    @Override
    public void apply(Transaction transaction, PublicKeyAnnouncementAppendix appendix, Account senderAccount, Account recipientAccount) {
        if (accountPublicKeyService.setOrVerifyPublicKey(recipientAccount.getId(), appendix.getPublicKey())) {
            accountPublicKeyService.apply(recipientAccount, appendix.getPublicKey());
        }
    }

    @Override
    public Class<PublicKeyAnnouncementAppendix> forClass() {
        return PublicKeyAnnouncementAppendix.class;
    }
}
