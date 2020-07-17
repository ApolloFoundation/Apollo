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

package com.apollocurrency.aplwallet.apl.core.entity.blockchain;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlPhasing;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.AccountControlType;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Filter;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Transaction {

    boolean isUnconfirmedDuplicate(Map<TransactionType, Map<String, Integer>> unconfirmedDuplicates);

    long getId();

    long getDbId();

    String getStringId();

    long getSenderId();

    byte[] getSenderPublicKey();

    boolean shouldSavePublicKey();

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

    void setFeeATM(long feeATM);

    default long[] getBackFees() {
        return new long[]{};
    }

    String getReferencedTransactionFullHash();

    byte[] referencedTransactionFullHash();

    void sign(Signature signature);

    Signature getSignature();

    String getFullHashString();

    byte[] getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    default byte[] getCopyTxBytes() {
        byte[] txBytes = bytes();
        return Arrays.copyOf(txBytes, txBytes.length);
    }

    byte[] bytes();

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

    /**
     * @deprecated see method with longer parameters list below
     */
    default boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        return false;
    }

    default boolean attachmentIsDuplicate(Map<TransactionType, Map<String, Integer>> duplicates,
                                          boolean atAcceptanceHeight,
                                          Set<AccountControlType> senderAccountControls,
                                          AccountControlPhasing accountControlPhasing) {
        return false;
    }

    interface Builder {

        Builder recipientId(long recipientId);

        default Builder recipientRs(String recipientRS) {
            return recipientId(Convert.parseAccountId(recipientRS));
        }

        Builder referencedTransactionFullHash(String referencedTransactionFullHash);

        Builder referencedTransactionFullHash(byte[] referencedTransactionFullHash);

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

        Builder signature(Signature signature);

        Transaction build() throws AplException.NotValidException;

        /**
         * Build transaction adn encrypt attachments. This method doesn't sign the transaction.
         * The transaction keeps unsigned.
         *
         * @param keySeed the key seed to encrypt appendixes
         * @return unsigned transaction with encrypted appendixes
         * @throws AplException.NotValidException
         */
        Transaction build(byte[] keySeed) throws AplException.NotValidException;

        TransactionImpl.BuilderImpl blockId(long blockId);
    }
}
