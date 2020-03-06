/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.CriticalUpdate;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.ImportantUpdate;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.MinorUpdate;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateV2Attachment;
import com.apollocurrency.aplwallet.apl.udpater.intfce.Level;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.Version;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

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
        UpdateAttachment attachment = (UpdateAttachment) transaction.getAttachment();
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
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.UPDATE_CRITICAL;
        }

        @Override
        public String getName() {
            return "CriticalUpdate";
        }

        @Override
        public CriticalUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new CriticalUpdate(buffer);
        }

        @Override
        public CriticalUpdate parseAttachment(JSONObject attachmentData) {
            return new CriticalUpdate(attachmentData);
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
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.UPDATE_IMPORTANT;
        }

        @Override
        public String getName() {
            return "ImportantUpdate";
        }

        @Override
        public ImportantUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ImportantUpdate(buffer);
        }

        @Override
        public ImportantUpdate parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ImportantUpdate(attachmentData);
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
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.UPDATE_MINOR;
        }

        @Override
        public String getName() {
            return "MinorUpdate";
        }

        @Override
        public MinorUpdate parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new MinorUpdate(buffer);
        }

        @Override
        public MinorUpdate parseAttachment(JSONObject attachmentData) {
            return new MinorUpdate(attachmentData);
        }
    };
    public static final TransactionType UPDATE_V2 = new Update() {
        @Override
        public Level getLevel() {
            throw new UnsupportedOperationException("Level is not defined for UpdateV2 statically");
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.NotValidException {
            UpdateV2Attachment attachment = (UpdateV2Attachment) transaction.getAttachment();
            Version version = attachment.getReleaseVersion();
            if (version.getMinorVersion() > Short.MAX_VALUE || version.getIntermediateVersion() > Short.MAX_VALUE || version.getMajorVersion() > Byte.MAX_VALUE) {
                throw new AplException.NotValidException("Update version is too big! " + version);
            }
        }

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_UPDATE_V2;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.UPDATE_V2;
        }

        @Override
        public String getName() {
            return "UpdateV2";
        }

        @Override
        public UpdateV2Attachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new UpdateV2Attachment(buffer);
        }

        @Override
        public UpdateV2Attachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new UpdateV2Attachment(attachmentData);
        }
    };

}
