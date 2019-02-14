/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.account.Account;
import com.apollocurrency.aplwallet.apl.core.account.AccountLedger;
import com.apollocurrency.aplwallet.apl.core.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.app.Genesis;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EmptyAttachment;
import com.apollocurrency.aplwallet.apl.util.AplException;
import java.nio.ByteBuffer;
import org.json.simple.JSONObject;

/**
 *
 * @author al
 */
public abstract class Payment extends TransactionType {
    
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
            Account.getAccount(Genesis.CREATOR_ID).addToBalanceAndUnconfirmedBalanceATM(getLedgerEvent(), transaction.getId(), transaction.getAmountATM());
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
            if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
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
            if (transaction.getAmountATM() <= 0 || transaction.getAmountATM() >= blockchainConfig.getCurrentConfig().getMaxBalanceATM()) {
                throw new AplException.NotValidException("Invalid private payment");
            }
        }
    };
    
}
