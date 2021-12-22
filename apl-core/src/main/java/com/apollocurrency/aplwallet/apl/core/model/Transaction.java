/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.model;

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

import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    /**
     * @return list of all transaction appendages, tx type attachment is first in the list
     */
    List<AbstractAppendix> getAppendages();

    int getECBlockHeight();

    long getECBlockId();

    boolean ofType(TransactionTypes.TransactionTypeSpec spec);

    default boolean isNotOfType(TransactionTypes.TransactionTypeSpec spec){
        return !ofType(spec);
    }

    /**
     * @return optional transaction error message, will be non-empty for failed transactions
     */
    Optional<String> getErrorMessage();

    /**
     * Fail transaction with the given message
     * @param message not blank message to fail this transaction with
     */
    void fail(String message);

    /**
     * @return true, if transaction is failed, otherwise - false
     */
    boolean isFailed();

    /**
     * @return true, if this transaction supports failures during execution, otherwise - false
     */
    boolean canFailDuringExecution();

    /**
     * Restore transaction status as not failed
     */
    void resetFail();

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

        Transaction build();

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

        Builder errorMessage(String errorMessage);
    }
}
