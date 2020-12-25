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
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptToSelfMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Transaction {

    boolean isUnconfirmedDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> unconfirmedDuplicates);

    long getId();

    long getDbId();

    String getStringId();

    long getSenderId();

    boolean hasValidSignature();

    void withValidSignature();

    byte[] getSenderPublicKey();

    long getRecipientId();

    int getHeight();

    void setHeight(int height);

    long getBlockId();

    Block getBlock();

    void setBlock(Block block);

    void unsetBlock();

    /**
     * the transaction sequence number in the block
     * @return the transaction sequence number in the block
     */
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

    int getECBlockHeight();

    long getECBlockId();

    /**
     * Transaction V3 properties
    */
    String getChainId();

    /**
     * the number of transactions sent by the sender
     * @return the number of transactions sent by the sender
     */
    BigInteger getNonce();

    BigInteger getAmount();//long:getAmountATM

    BigInteger getFuelPrice();
    BigInteger getFuelLimit();

    long getLongTimestamp();//int getTimestamp

    /**
     * Return RLP encoded transaction bytes.
     * The same as {@link #bytes()} for V2 transaction
     * @return byte array of the RLP encoded transaction
     */
    byte[] rlpEncodedTx();

    /**
     * Return RLP encoded unsigned transaction bytes.
     * The same as {@link #getUnsignedBytes()}} for V2 transaction
     * @return byte array of the RLP encoded transaction
     */
    byte[] rlpEncodedUnsignedTx();

    /**
     *
     */
    boolean ofType(TransactionTypes.TransactionTypeSpec spec);

    default boolean isNotOfType(TransactionTypes.TransactionTypeSpec spec){
        return !ofType(spec);
    }

    /**
     * @deprecated see method with longer parameters list below
     */
    default boolean attachmentIsDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, boolean atAcceptanceHeight) {
        return false;
    }

    default boolean attachmentIsDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates,
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

        Builder appendix(AbstractAttachment attachment);

        Builder appendix(AbstractAppendix appendix);

        Builder appendix(MessageAppendix message);

        Builder appendix(EncryptedMessageAppendix encryptedMessage);

        Builder appendix(EncryptToSelfMessageAppendix encryptToSelfMessage);

        Builder appendix(PublicKeyAnnouncementAppendix publicKeyAnnouncement);

        Builder appendix(PrunablePlainMessageAppendix prunablePlainMessage);

        Builder appendix(PrunableEncryptedMessageAppendix prunableEncryptedMessage);

        Builder appendix(PhasingAppendix phasing);

        Builder timestamp(int timestamp);

        Builder ecBlockHeight(int height);

        Builder ecBlockData(EcBlockData ecBlockData);

        Builder dbId(long dbId);

        Builder ecBlockId(long blockId);

        Builder signature(Signature signature);

        Transaction build() throws AplException.NotValidException;

        TransactionImpl.BuilderImpl blockId(long blockId);
    }
}
