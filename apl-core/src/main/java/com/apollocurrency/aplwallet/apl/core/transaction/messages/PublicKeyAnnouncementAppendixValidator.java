/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;

@Singleton
public class PublicKeyAnnouncementAppendixValidator extends AbstractAppendixValidator<PublicKeyAnnouncementAppendix> {
    private final AccountPublicKeyService accountPublicKeyService;

    @Inject
    public PublicKeyAnnouncementAppendixValidator(AccountPublicKeyService accountPublicKeyService) {
        this.accountPublicKeyService = accountPublicKeyService;
    }

    @Override
    public void validateStateDependent(Transaction transaction, PublicKeyAnnouncementAppendix appendix, int validationHeight) throws AplException.ValidationException {
        byte[] recipientPublicKey = accountPublicKeyService.getPublicKeyByteArray(transaction.getRecipientId());
        if (recipientPublicKey != null && !Arrays.equals(appendix.getPublicKey(), recipientPublicKey)) {
            throw new AplException.NotCurrentlyValidException("A different public key for this account has already been announced");
        }
    }

    @Override
    public void validateStateIndependent(Transaction transaction, PublicKeyAnnouncementAppendix appendix, int validationHeight) throws AplException.ValidationException {
        if (transaction.getRecipientId() == 0) {
            throw new AplException.NotValidException("PublicKeyAnnouncement cannot be attached to transactions with no recipient");
        }
        byte[] publicKey = appendix.getPublicKey();
        if (!Crypto.isCanonicalPublicKey(publicKey)) {
            throw new AplException.NotValidException("Invalid recipient public key: " + Convert.toHexString(publicKey));
        }
        long recipientId = transaction.getRecipientId();
        if (AccountService.getId(publicKey) != recipientId) {
            throw new AplException.NotValidException("Announced public key does not match recipient accountId");
        }
    }

    @Override
    public Class<PublicKeyAnnouncementAppendix> forClass() {
        return PublicKeyAnnouncementAppendix.class;
    }
}
