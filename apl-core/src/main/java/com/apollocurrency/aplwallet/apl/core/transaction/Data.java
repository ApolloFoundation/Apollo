/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.TaggedData;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionImpl;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataExtend;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.TaggedDataUpload;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public abstract class Data extends TransactionType {
    
    private static final Fee TAGGED_DATA_FEE = new Fee.SizeBasedFee(Constants.ONE_APL, Constants.ONE_APL / 10) {
        @Override
        public int getSize(TransactionImpl transaction, Appendix appendix) {
            return appendix.getFullSize();
        }
    };

    private Data() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_DATA;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return TAGGED_DATA_FEE;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public final boolean canHaveRecipient() {
        return false;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

    @Override
    public final boolean isPhasable() {
        return false;
    }
    public static final TransactionType TAGGED_DATA_UPLOAD = new Data() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_DATA_TAGGED_DATA_UPLOAD;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.TAGGED_DATA_UPLOAD;
        }

        @Override
        public TaggedDataUpload parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new TaggedDataUpload(buffer);
        }

        @Override
        public TaggedDataUpload parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new TaggedDataUpload(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            TaggedDataUpload attachment = (TaggedDataUpload) transaction.getAttachment();
            if (attachment.getData() == null && timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMinPrunableLifetime()) {
                throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
            }
            if (attachment.getData() != null) {
                if (attachment.getName().length() == 0 || attachment.getName().length() > Constants.MAX_TAGGED_DATA_NAME_LENGTH) {
                    throw new AplException.NotValidException("Invalid name length: " + attachment.getName().length());
                }
                if (attachment.getDescription().length() > Constants.MAX_TAGGED_DATA_DESCRIPTION_LENGTH) {
                    throw new AplException.NotValidException("Invalid description length: " + attachment.getDescription().length());
                }
                if (attachment.getTags().length() > Constants.MAX_TAGGED_DATA_TAGS_LENGTH) {
                    throw new AplException.NotValidException("Invalid tags length: " + attachment.getTags().length());
                }
                if (attachment.getType().length() > Constants.MAX_TAGGED_DATA_TYPE_LENGTH) {
                    throw new AplException.NotValidException("Invalid type length: " + attachment.getType().length());
                }
                if (attachment.getChannel().length() > Constants.MAX_TAGGED_DATA_CHANNEL_LENGTH) {
                    throw new AplException.NotValidException("Invalid channel length: " + attachment.getChannel().length());
                }
                if (attachment.getFilename().length() > Constants.MAX_TAGGED_DATA_FILENAME_LENGTH) {
                    throw new AplException.NotValidException("Invalid filename length: " + attachment.getFilename().length());
                }
                if (attachment.getData().length == 0 || attachment.getData().length > Constants.MAX_TAGGED_DATA_DATA_LENGTH) {
                    throw new AplException.NotValidException("Invalid data length: " + attachment.getData().length);
                }
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            TaggedDataUpload attachment = (TaggedDataUpload) transaction.getAttachment();
            TaggedData.add((TransactionImpl) transaction, attachment);
        }

        @Override
        public String getName() {
            return "TaggedDataUpload";
        }

        @Override
        public boolean isPruned(long transactionId) {
            return TaggedData.isPruned(transactionId);
        }
    };
    public static final TransactionType TAGGED_DATA_EXTEND = new Data() {
        @Override
        public byte getSubtype() {
            return SUBTYPE_DATA_TAGGED_DATA_EXTEND;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.TAGGED_DATA_EXTEND;
        }

        @Override
        public TaggedDataExtend parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new TaggedDataExtend(buffer);
        }

        @Override
        public TaggedDataExtend parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new TaggedDataExtend(attachmentData);
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            TaggedDataExtend attachment = (TaggedDataExtend) transaction.getAttachment();
            if ((attachment.jsonIsPruned() || attachment.getData() == null) && timeService.getEpochTime() - transaction.getTimestamp() < blockchainConfig.getMinPrunableLifetime()) {
                throw new AplException.NotCurrentlyValidException("Data has been pruned prematurely");
            }
            Transaction uploadTransaction = blockchain.findTransaction(attachment.getTaggedDataId(), blockchain.getHeight());
            if (uploadTransaction == null) {
                throw new AplException.NotCurrentlyValidException("No such tagged data upload " + Long.toUnsignedString(attachment.getTaggedDataId()));
            }
            if (uploadTransaction.getType() != TAGGED_DATA_UPLOAD) {
                throw new AplException.NotValidException("Transaction " + Long.toUnsignedString(attachment.getTaggedDataId()) + " is not a tagged data upload");
            }
            if (attachment.getData() != null) {
                TaggedDataUpload taggedDataUpload = (TaggedDataUpload) uploadTransaction.getAttachment();
                if (!Arrays.equals(attachment.getHash(), taggedDataUpload.getHash())) {
                    throw new AplException.NotValidException("Hashes don't match! Extend hash: " + Convert.toHexString(attachment.getHash()) + " upload hash: " + Convert.toHexString(taggedDataUpload.getHash()));
                }
            }
            TaggedData taggedData = TaggedData.getData(attachment.getTaggedDataId());
            if (taggedData != null && taggedData.getTransactionTimestamp() > timeService.getEpochTime() + 6 * blockchainConfig.getMinPrunableLifetime()) {
                throw new AplException.NotCurrentlyValidException("Data already extended, timestamp is " + taggedData.getTransactionTimestamp());
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            TaggedDataExtend attachment = (TaggedDataExtend) transaction.getAttachment();
            TaggedData.extend(transaction, attachment);
        }

        @Override
        public String getName() {
            return "TaggedDataExtend";
        }

        @Override
        public boolean isPruned(long transactionId) {
            return false;
        }
    };
    
}
