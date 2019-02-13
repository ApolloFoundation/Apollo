/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.app.transaction;

import com.apollocurrency.aplwallet.apl.core.app.Account;
import com.apollocurrency.aplwallet.apl.core.app.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public abstract class Update extends TransactionType {
    
    private final Fee UPDATE_FEE = new Fee.ConstantFee(Constants.ONE_APL);

    private Update() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_UPDATE;
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
        return true;
    }

    @Override
    public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
        Attachment.UpdateAttachment attachment = (Attachment.UpdateAttachment) transaction.getAttachment();
        if (attachment.getUrl().getFirst().length != Constants.UPDATE_URL_PART_LENGTH || attachment.getUrl().getSecond().length != Constants.UPDATE_URL_PART_LENGTH || attachment.getHash().length > Constants.MAX_UPDATE_HASH_LENGTH) {
            throw new AplException.NotValidException("Invalid update transaction attachment:" + attachment.getJSONObject());
        }
    }

    public static boolean isUpdate(TransactionType transactionType) {
        return transactionType.getType() == TYPE_UPDATE;
    }

    public abstract Level getLevel();

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return UPDATE_FEE;
    }
    public static final TransactionType CRITICAL = new Update() {
        @Override
        public Level getLevel() {
            return Level.CRITICAL;
        }

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_UPDATE_CRITICAL;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.UPDATE_CRITICAL;
        }

        @Override
        public String getName() {
            return "CriticalUpdate";
        }

        @Override
        public Attachment.CriticalUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.CriticalUpdate(buffer);
        }

        @Override
        public Attachment.CriticalUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.CriticalUpdate(attachmentData);
        }
    };
    public static final TransactionType IMPORTANT = new Update() {
        @Override
        public Level getLevel() {
            return Level.IMPORTANT;
        }

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_UPDATE_IMPORTANT;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.UPDATE_IMPORTANT;
        }

        @Override
        public String getName() {
            return "ImportantUpdate";
        }

        @Override
        public Attachment.ImportantUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.ImportantUpdate(buffer);
        }

        @Override
        public Attachment.ImportantUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.ImportantUpdate(attachmentData);
        }
    };
    public static final TransactionType MINOR = new Update() {
        @Override
        public Level getLevel() {
            return Level.MINOR;
        }

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_UPDATE_MINOR;
        }

        @Override
        public AccountLedger.LedgerEvent getLedgerEvent() {
            return AccountLedger.LedgerEvent.UPDATE_MINOR;
        }

        @Override
        public String getName() {
            return "MinorUpdate";
        }

        @Override
        public Attachment.MinorUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new Attachment.MinorUpdate(buffer);
        }

        @Override
        public Attachment.MinorUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new Attachment.MinorUpdate(attachmentData);
        }
    };
    
}
