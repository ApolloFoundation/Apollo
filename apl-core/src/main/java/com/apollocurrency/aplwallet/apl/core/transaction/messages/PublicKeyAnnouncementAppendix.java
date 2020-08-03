/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.messages;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.impl.AccountPublicKeyServiceImpl;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

public class PublicKeyAnnouncementAppendix extends AbstractAppendix {

    private static final String appendixName = "PublicKeyAnnouncement";
    private static AccountPublicKeyService accountPublicKeyService;
    private final byte[] publicKey;

    public PublicKeyAnnouncementAppendix(ByteBuffer buffer) {
        super(buffer);
        this.publicKey = new byte[32];
        buffer.get(this.publicKey);
    }

    public PublicKeyAnnouncementAppendix(JSONObject attachmentData) {
        super(attachmentData);
        this.publicKey = Convert.parseHexString((String) attachmentData.get("recipientPublicKey"));
    }

    public PublicKeyAnnouncementAppendix(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public static PublicKeyAnnouncementAppendix parse(JSONObject attachmentData) {
        if (!Appendix.hasAppendix(appendixName, attachmentData)) {
            return null;
        }
        return new PublicKeyAnnouncementAppendix(attachmentData);
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
        throw new UnsupportedOperationException("Validation is not supported, use separate class");
    }

    @Override
    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        throw new UnsupportedOperationException("Apply is not supported, use separate class");
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

}
