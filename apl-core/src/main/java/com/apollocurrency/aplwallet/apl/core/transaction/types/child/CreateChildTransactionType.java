/*
 *  Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.child;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ChildAccountAttachment;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.ByteBuffer;
@Singleton
@Slf4j
public class CreateChildTransactionType extends ChildAccountTransactionType {
    private final AccountPublicKeyService accountPublicKeyService;
    private final Blockchain blockchain;

    @Inject
    public CreateChildTransactionType(BlockchainConfig blockchainConfig, AccountService accountService, AccountPublicKeyService accountPublicKeyService, Blockchain blockchain) {
        super(blockchainConfig, accountService);
        this.accountPublicKeyService = accountPublicKeyService;
        this.blockchain = blockchain;
    }

    @Override
    public void doStateDependentValidation(Transaction transaction) throws AplException.NotValidException {
        super.doStateDependentValidation(transaction);
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        for (byte[] childPublicKey : attachment.getChildPublicKey()) {
            if (isAccountExists(childPublicKey)) {
                throw new AplException.NotValidException("Child account already exists, publicKey="
                    + Convert.toHexString(childPublicKey));
            }
        }
    }

    @Override
    public void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException {
        if (getBlockchainConfig().isTransactionV2ActiveAtHeight(blockchain.getHeight())) {
            throw new AplException.NotYetEnabledException("Transactions of Type 'CreateChildAccount' are not yet enabled");
        }
        super.doStateIndependentValidation(transaction);
    }

    @Override
    public void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        super.applyAttachment(transaction, senderAccount, recipientAccount);
        Account childAccount;
        ChildAccountAttachment attachment = (ChildAccountAttachment) transaction.getAttachment();
        log.trace("CREATE_CHILD: parentId={}, child count={}", senderAccount.getId(), attachment.getChildPublicKey().size());
        for (byte[] childPublicKey : attachment.getChildPublicKey()) {
            //create an account
            childAccount = getAccountService().createAccount(AccountService.getId(childPublicKey));
            childAccount.setParentId(senderAccount.getId());
            childAccount.setAddrScope(attachment.getAddressScope());
            childAccount.setMultiSig(true);
            log.trace("CREATE_CHILD: create ParentId={}, childRS={}, childId={}, child publickey={}",
                senderAccount.getId(),
                Convert2.defaultRsAccount(childAccount.getId()),
                childAccount.getId(),
                Convert.toHexString(childPublicKey));
            //save the account into db
            getAccountService().update(childAccount, false);
            log.trace("CREATE_CHILD: update child={}", childAccount);
            //save the public key into db
            log.trace("CREATE_CHILD: apply public key child={}", childAccount);
            accountPublicKeyService.apply(childAccount, childPublicKey);

        }
    }

    @Override
    public TransactionTypes.TransactionTypeSpec getSpec() {
        return TransactionTypes.TransactionTypeSpec.CHILD_ACCOUNT_CREATE;
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
}
