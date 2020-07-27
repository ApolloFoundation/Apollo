/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;

/**
 * Create child account transaction.
 * Sender is a parent account. There is an list of child public keys in the attachment.
 * @author andrii.zinchenko@firstbridge.io
 */
@Slf4j
public abstract class ChildAccount extends TransactionType {

    private static boolean isAccountExists(byte[] childPublicKey) {
        return lookupAccountService().getAccount(childPublicKey) != null;
    }

    public static final TransactionType CREATE_CHILD = new ChildAccount() {
        @Override
        public void validateAttachment(Transaction transaction) throws AplException.NotValidException {
            super.validateAttachment(transaction);
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
            for (byte[] childPublicKey : attachment.getChildPublicKey()) {
                if (ChildAccount.isAccountExists(childPublicKey)) {
                    throw new AplException.NotValidException("Child account already exists, publicKey="
                        +Convert.toHexString(childPublicKey));
                }
            }
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            super.applyAttachment(transaction, senderAccount, recipientAccount);
            Account childAccount;
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
            log.trace("CREATE_CHILD: parentId={}, child count={}", senderAccount.getId(), attachment.getChildPublicKey().size());
            for (byte[] childPublicKey : attachment.getChildPublicKey()) {
                //create an account
                childAccount = lookupAccountService().addOrGetAccount(AccountService.getId(childPublicKey));
                childAccount.setParentId(senderAccount.getId());
                childAccount.setAddrScope(attachment.getAddressScope());
                childAccount.setMultiSig(true);
                log.trace("CREATE_CHILD: create ParentId={}, childRS={}, childId={}, child publickey={}",
                    senderAccount.getId(),
                    Convert.defaultRsAccount(childAccount.getId()),
                    childAccount.getId(),
                    Convert.toHexString(childPublicKey));
                //save the account into db
                lookupAccountService().update(childAccount, false);
                log.trace("CREATE_CHILD: update child={}", childAccount);
                //save the public key into db
                log.trace("CREATE_CHILD: apply public key child={}", childAccount);
                ChildAccount.lookupAccountPublicKeyService().apply(childAccount, childPublicKey);

            }
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
            super.validateAttachment(transaction);
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
            if (attachment.getChildCount() != 1){
                throw new AplException.NotValidException("Wrong value of the child count value, only one account can be converted at once.");
            }
            throw new AplException.NotValidException("Not implemented yet.");
        }

        @Override
        public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
            super.applyAttachment(transaction, senderAccount, recipientAccount);
            ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
            //TODO: not implemented yet
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
    public void validateAttachment(Transaction transaction) throws AplException.NotValidException {
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        if (transaction.getAmountATM() != 0 ) {
            throw new AplException.NotValidException("Wrong value of the transaction amount "+transaction.getAmountATM());
        }
        if (attachment.getChildCount() <= 0 || attachment.getChildCount() != attachment.getChildPublicKey().size()){
            throw new AplException.NotValidException("Wrong value of the child count, count=" + attachment.getChildCount());
        }
        for (byte[] publicKey : attachment.getChildPublicKey()) {
            if(Arrays.equals(publicKey, transaction.getSenderPublicKey())) {
                throw new AplException.NotValidException("Wrong value of the child public keys, a child can't simultaneously be a parent.");
            }
            if (!Crypto.isCanonicalPublicKey(publicKey)) {
                throw new AplException.NotValidException("Invalid child public key: " + Convert.toHexString(publicKey));
            }
        }
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {

    }

    @Override
    public final boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
        return true;
    }

    @Override
    public void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount) {
    }

    @Override
    public boolean isDuplicate(Transaction transaction, Map<TransactionType, Map<String, Integer>> duplicates) {
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        for (byte[] publicKey : attachment.getChildPublicKey()) {
            if (TransactionType.isDuplicate(CREATE_CHILD, Convert.toHexString(publicKey), duplicates, true)) {
                return true;
            }
        }
        return super.isDuplicate(transaction, duplicates);
    }

    @Override
    public final boolean canHaveRecipient() {
        return true;
    }

    @Override
    public final boolean isPhasingSafe() {
        return false;
    }

    @Override
    public boolean isPhasable() {
        return false;
    }

    @Override
    public Fee getBaselineFee(Transaction transaction) {
        return TX_FEE;
    }

}
