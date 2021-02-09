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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.blockchain;

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
import com.apollocurrency.aplwallet.apl.util.exception.AplException;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Transaction {

    Transaction getTransactionImpl();

    boolean isUnconfirmedDuplicate(Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> unconfirmedDuplicates);

    long getId();

    String getStringId();

    long getSenderId();

    boolean hasValidSignature();

    default boolean isValidSignature() {
        return hasValidSignature();
    }

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

    Signature getSignature();

    String getFullHashString();

    byte[] getFullHash();

    TransactionType getType();

    Attachment getAttachment();

    byte getVersion();

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

        Transaction build() throws AplException.NotValidException;

        Builder id(long id);

        Builder signature(Signature signature);

        Builder recipientId(long recipientId);

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

        Builder ecBlockId(long blockId);

        Builder blockId(long blockId);

        Builder height(int height);

        Builder senderId(long senderId);

        Builder fullHash(byte[] fullHash);

        Builder blockTimestamp(int blockTimestamp);

        Builder index(short index);
    }
}
