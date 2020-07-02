/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.app.Fee;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.model.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.update.UpdateAttachment;
import com.apollocurrency.aplwallet.apl.util.Constants;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author andrii.zinchenko@firstbridge.io
 */
public abstract class ChildAccount extends TransactionType {

    public static final TransactionType CREATE_CHILD = new ChildAccount() {
        @Override
        public void validateAttachment(Transaction transaction) throws AplException.NotValidException {
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
            if (attachment.getChildCount() <= 0){
                throw new AplException.NotValidException("Wrong value: childCount=" + attachment.getChildCount());
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            super.applyAttachment(transaction, senderAccount, recipientAccount);
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        }

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_CHILD_CREATE;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CHILD_CREATE;
        }

        @Override
        public String getName() {
            return "CreateChildAccount";
        }

        @Override
        public ChildAccountAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ChildAccountAttachment(buffer);
        }

        @Override
        public ChildAccountAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ChildAccountAttachment(attachmentData);
        }
    };

    public static final TransactionType CONVERT_TO_CHILD = new ChildAccount() {
        @Override
        public void validateAttachment(Transaction transaction) throws AplException.NotValidException {
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
            if (attachment.getChildCount() <= 0){
                throw new AplException.NotValidException("Wrong value: childCount=" + attachment.getChildCount());
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            super.applyAttachment(transaction, senderAccount, recipientAccount);
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        }

        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_CHILD_CONVERT_TO;
        }

        @Override
        public LedgerEvent getLedgerEvent() {
            return LedgerEvent.CHILD_CONVERT_TO;
        }

        @Override
        public String getName() {
            return "ConvertToChildAccount";
        }

        @Override
        public ChildAccountAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return new ChildAccountAttachment(buffer);
        }

        @Override
        public ChildAccountAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return new ChildAccountAttachment(attachmentData);
        }
    };
    private static final Fee TX_FEE = new Fee.ConstantFee(Constants.ONE_APL);

    private ChildAccount() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_CHILD_ACCOUNT;
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
        return true;
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

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return TX_FEE;
    }

}
