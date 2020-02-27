/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import java.nio.ByteBuffer;
import java.util.Arrays;

import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.core.account.service.AccountService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import javax.enterprise.inject.spi.CDI;

public class PublicKeyAnnouncementAppendix extends AbstractAppendix {

    private static final String appendixName = "PublicKeyAnnouncement";

    public static PublicKeyAnnouncementAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        return new PublicKeyAnnouncementAppendix(attachmentData);
    }
    private static AccountPublicKeyService accountPublicKeyService;

    protected AccountPublicKeyService lookupAccountPublickKeyService(){
        if ( accountPublicKeyService == null) {
            accountPublicKeyService = CDI.current().select(AccountPublicKeyServiceImpl.class).get();
        }
        return accountPublicKeyService;
    }

    private final byte[] publicKey;

    public PublicKeyAnnouncementAppendix(ByteBuffer buffer) {
        super(buffer);
        this.publicKey = new byte[32];
        buffer.get(this.publicKey);
    }

    public PublicKeyAnnouncementAppendix(JSONObject attachmentData) {
        super(attachmentData);
        this.publicKey = Convert.parseHexString((String)attachmentData.get("recipientPublicKey"));
    }

    public PublicKeyAnnouncementAppendix(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    @Override
    public String getAppendixName() {
        return appendixName;
    }

    @Override
    public int getMySize() {
        return 32;
    }

    @Override
    public void putMyBytes(ByteBuffer buffer) {
        buffer.put(publicKey);
    }

    @Override
    public void putMyJSON(JSONObject json) {
        json.put("recipientPublicKey", Convert.toHexString(publicKey));
    }

    @Override
    public void validate(Transaction transaction, int blockHeight) throws AplException.ValidationException {
        if (transaction.getRecipientId() == 0) {
            throw new AplException.NotValidException("PublicKeyAnnouncement cannot be attached to transactions with no recipient");
        }
        if (!Crypto.isCanonicalPublicKey(publicKey)) {
            throw new AplException.NotValidException("Invalid recipient public key: " + Convert.toHexString(publicKey));
        }
        long recipientId = transaction.getRecipientId();
        if (AccountService.getId(this.publicKey) != recipientId) {
            throw new AplException.NotValidException("Announced public key does not match recipient accountId");
        }
        byte[] recipientPublicKey = lookupAccountPublickKeyService().getPublicKeyByteArray(recipientId);
        if (recipientPublicKey != null && ! Arrays.equals(publicKey, recipientPublicKey)) {
            throw new AplException.NotCurrentlyValidException("A different public key for this account has already been announced");
        }
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (lookupAccountPublickKeyService().setOrVerifyPublicKey(recipientAccount.getId(), publicKey)) {
            lookupAccountPublickKeyService().apply(recipientAccount, this.publicKey);
        }
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

}
