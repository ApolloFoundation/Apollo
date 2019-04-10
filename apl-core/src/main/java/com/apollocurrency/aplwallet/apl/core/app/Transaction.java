/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

public interface Transaction {

    static Transaction.Builder newTransactionBuilder(byte[] senderPublicKey, long amountATM, long feeATM, short deadline, Attachment attachment, int timestamp) {
        return new TransactionImpl.BuilderImpl((byte)1, senderPublicKey, amountATM, feeATM, deadline, (AbstractAttachment)attachment, timestamp);
    }

    static Transaction.Builder newTransactionBuilder(byte[] transactionBytes) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes);
    }

    static Transaction.Builder newTransactionBuilder(JSONObject transactionJSON) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionJSON);
    }

    static Transaction.Builder newTransactionBuilder(byte[] transactionBytes, JSONObject prunableAttachments) throws AplException.NotValidException {
        return TransactionImpl.newTransactionBuilder(transactionBytes, prunableAttachments);
    }

    interface Builder {

        Builder recipientId(long recipientId);

        default Builder recipientRs(String recipientRS) {return recipientId(Convert.parseAccountId(recipientRS));}

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder appendix(MessageAppendix message);

        Builder appendix(EncryptedMessageAppendix encryptedMessage);

        Builder appendix(EncryptToSelfMessageAppendix encryptToSelfMessage);

        Builder appendix(PublicKeyAnnouncementAppendix publicKeyAnnouncement);

        Builder appendix(PrunablePlainMessageAppendix prunablePlainMessage);

        Builder appendix(PrunableEncryptedMessageAppendix prunableEncryptedMessage);

        Builder appendix(PhasingAppendix phasing);

        Builder timestamp(int timestamp);

        Builder ecBlockHeight(int height);

        Builder dbId(long dbId);

        Builder ecBlockId(long blockId);

        Transaction build() throws AplException.NotValidException;

        Transaction build(byte[] keySeed) throws AplException.NotValidException;

    }

    void setFeeATM(long feeATM);

    long getId();

    long getDbId();

    String getStringId();

    long getSenderId();

    byte[] getSenderPublicKey();

    long getRecipientId();

    int getHeight();

    void setHeight(int height);

    long getBlockId();

    Block getBlock();

    void setBlock(Block block);

    void unsetBlock();

    short getIndex();

    void setIndex(int index);

    int getTimestamp();

    int getBlockTimestamp();

    short getDeadline();

    int getExpiration();

    long getAmountATM();

    long getFeeATM();

    String getReferencedTransactionFullHash();

    byte[] referencedTransactionFullHash();

    byte[] getSignature();

    String getFullHashString();

    byte[] getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    boolean verifySignature();

    byte[] getBytes();

    byte[] getUnsignedBytes();

    JSONObject getJSONObject();

    JSONObject getPrunableAttachmentJSON();

    byte getVersion();

    int getFullSize();

    MessageAppendix getMessage();

    EncryptedMessageAppendix getEncryptedMessage();

    EncryptToSelfMessageAppendix getEncryptToSelfMessage();

    PhasingAppendix getPhasing();

    boolean attachmentIsPhased();

    PublicKeyAnnouncementAppendix getPublicKeyAnnouncement();

    PrunablePlainMessageAppendix getPrunablePlainMessage();

    boolean hasPrunablePlainMessage();

    PrunableEncryptedMessageAppendix getPrunableEncryptedMessage();

    boolean hasPrunableEncryptedMessage();

    List<AbstractAppendix> getAppendages();

    List<AbstractAppendix> getAppendages(boolean includeExpiredPrunable);

    List<AbstractAppendix> getAppendages(Filter<Appendix> filter, boolean includeExpiredPrunable);

    int getECBlockHeight();

    long getECBlockId();

    default boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        return false;
    }
}
