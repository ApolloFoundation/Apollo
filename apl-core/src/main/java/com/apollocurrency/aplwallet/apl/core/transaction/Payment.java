/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.account.model.Account;
import com.apollocurrency.aplwallet.apl.core.app.GenesisImporter;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.util.AplException;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;

/**
 * @author al
 */
public abstract class Payment extends TransactionType {

    public static final TransactionType ORDINARY = new Payment() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_PAYMENT_ORDINARY_PAYMENT;
        }

        @Override
        public final LedgerEvent getLedgerEvent() {
            return LedgerEvent.ORDINARY_PAYMENT;
        }

        @Override
        public String getName() {
            return "OrdinaryPayment";
        }

        @Override
        public EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return Attachment.ORDINARY_PAYMENT;
        }

        @Override
        public EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return Attachment.ORDINARY_PAYMENT;
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= lookupBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException("Invalid ordinary payment");
            }
        }
    };
    public static final TransactionType PRIVATE = new Payment() {
        @Override
        public final byte getSubtype() {
            return TransactionType.SUBTYPE_PAYMENT_PRIVATE_PAYMENT;
        }

        @Override
        public final LedgerEvent getLedgerEvent() {
            return LedgerEvent.PRIVATE_PAYMENT;
        }

        @Override
        public String getName() {
            return "PrivatePayment";
        }

        @Override
        public EmptyAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException {
            return Attachment.PRIVATE_PAYMENT;
        }

        @Override
        public EmptyAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException {
            return Attachment.PRIVATE_PAYMENT;
        }

        @Override
        public void validateAttachment(Transaction transaction) throws AplException.ValidationException {
            if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= lookupBlockchainConfig().getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException("Invalid private payment");
            }
        }
    };

    private Payment() {
    }

    @Override
    public final byte getType() {
        return TransactionType.TYPE_PAYMENT;
    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public final void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (recipientAccount == null) {
            lookupAccountService().addToBalanceAndUnconfirmedBalanceATM(lookupAccountService().getAccount(GenesisImporter.CREATOR_ID), getLedgerEvent(), transaction.getId(), transaction.getAmountATM());
        }
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

}
