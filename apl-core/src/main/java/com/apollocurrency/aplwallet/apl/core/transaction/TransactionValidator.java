/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.antifraud.AntifraudValidator;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Credential;
import com.apollocurrency.aplwallet.apl.core.signature.KeyValidator;
import com.apollocurrency.aplwallet.apl.core.signature.MultiSigCredential;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureCredential;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureToolFactory;
import com.apollocurrency.aplwallet.apl.core.signature.SignatureVerifier;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.utils.Convert2;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.annotation.ParentChildSpecific;
import com.apollocurrency.aplwallet.apl.util.annotation.ParentMarker;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class TransactionValidator {
    private final BlockchainConfig blockchainConfig;
    private final PhasingPollService phasingPollService;
    private final Blockchain blockchain;
    private final FeeCalculator feeCalculator;
    private final AccountPublicKeyService accountPublicKeyService;
    private final AccountControlPhasingService accountControlPhasingService;
    private final PrunableLoadingService prunableService;
    private final AccountService accountService;
    private final TransactionVersionValidator transactionVersionValidator;
    private final KeyValidator keyValidator;
    private final AppendixValidatorRegistry validatorRegistry;

    @Inject
    public TransactionValidator(BlockchainConfig blockchainConfig, PhasingPollService phasingPollService,
                                Blockchain blockchain, FeeCalculator feeCalculator, AccountService accountService,
                                AccountPublicKeyService accountPublicKeyService, AccountControlPhasingService accountControlPhasingService, TransactionVersionValidator transactionVersionValidator, PrunableLoadingService prunableService, AppendixValidatorRegistry validatorRegistry) {
        this.blockchainConfig = blockchainConfig;
        this.phasingPollService = phasingPollService;
        this.blockchain = blockchain;
        this.feeCalculator = feeCalculator;
        this.accountPublicKeyService = accountPublicKeyService;
        this.accountControlPhasingService = accountControlPhasingService;
        this.prunableService = prunableService;
        this.accountService = accountService;
        this.transactionVersionValidator = transactionVersionValidator;
        this.keyValidator = new PublicKeyValidator(accountPublicKeyService);
        this.validatorRegistry = validatorRegistry;
    }


    public int getFinishValidationHeight(Transaction transaction, Attachment attachment) {
        return attachment.isPhased(transaction) ? transaction.getPhasing().getFinishHeight() - 1 : blockchain.getHeight();
    }


    public void validate(Transaction transaction) throws AplException.ValidationException {
        if (!transactionVersionValidator.isValidVersion(transaction)) {
            throw new AplException.NotValidException("Unsupported transaction version:" + transaction.getVersion() + " at height " + blockchain.getHeight());
        }
        long maxBalanceAtm = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        short deadline = transaction.getDeadline();
        long feeATM = transaction.getFeeATM();
        long amountATM = transaction.getAmountATM();
        TransactionType type = transaction.getType();
        TransactionTypes.TransactionTypeSpec typeSpec = type.getSpec();
        if (transaction.getTimestamp() == 0 ? (deadline != 0 || feeATM != 0) : (deadline < 1 || feeATM < 0)
            || feeATM > maxBalanceAtm
            || amountATM < 0
            || amountATM > maxBalanceAtm
            || typeSpec == null) {
            throw new AplException.NotValidException("Invalid transaction parameters:\n type: " + type + ", timestamp: " + transaction.getTimestamp()
                + ", deadline: " + deadline + ", fee: " + feeATM + ", amount: " + amountATM);
        }
        byte[] referencedTransactionFullHash = Convert.parseHexString(transaction.getReferencedTransactionFullHash());

        if (referencedTransactionFullHash != null && referencedTransactionFullHash.length != 32) {
            throw new AplException.NotValidException("Invalid referenced transaction full hash " + Convert.toHexString(referencedTransactionFullHash));
        }
        Attachment attachment = transaction.getAttachment();

        if (attachment == null || typeSpec != attachment.getTransactionTypeSpec()) {
            throw new AplException.NotValidException("Invalid attachment " + attachment + " for transaction of type " + type);
        }

        long recipientId = transaction.getRecipientId();
        if (!type.canHaveRecipient() && (recipientId != 0 || amountATM != 0)) {
            throw new AplException.NotValidException("Transactions of this type must have recipient == 0, amount == 0");
        }

        if (type.mustHaveRecipient() && recipientId == 0) {
            throw new AplException.NotValidException("Transactions of this type must have a valid recipient");
        }

        if (!AntifraudValidator.validate(blockchain.getHeight(), blockchainConfig.getChain().getChainId(), transaction.getSenderId(),
            transaction.getRecipientId())) throw new AplException.NotValidException("Incorrect Passphrase");

        Account sender = accountService.getAccount(transaction.getSenderId());
        if (sender != null && sender.isChild()) {
            Account recipient = accountService.getAccount(transaction.getRecipientId());
            if (recipient == null) {
                throw new AplException.NotCurrentlyValidException("Account " + transaction.getRecipientId() + " does not exist yet.");
            }
            @ParentChildSpecific(ParentMarker.ADDRESS_RESTRICTION)
            boolean rc;
            switch (sender.getAddrScope()) {
                case IN_FAMILY:
                    rc = sender.getParentId() == recipient.getId() || sender.getParentId() == recipient.getParentId();
                    break;
                case EXTERNAL:
                    rc = true;
                    break;
                case CUSTOM:
                default:
                    throw new AplException.NotCurrentlyValidException("Unsupported value " +
                        sender.getAddrScope().name() +
                        " for sender address scope;" +
                        "sender.Id=" + sender.getId());
            }
            if (!rc) {
                throw new AplException.NotCurrentlyValidException("The parent account for sender and recipient must be the same;" +
                    "sender.parentId=" + sender.getParentId() + ", recipient.parentId=" + recipient.getParentId());
            }
        }

        boolean validatingAtFinish = transaction.getPhasing() != null && transaction.getSignature() != null && phasingPollService.getPoll(transaction.getId()) != null;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            prunableService.loadPrunable(transaction, appendage, false);
            //TODO Why does it need? Take a look how to use it.
            //if (! appendage.verifyVersion()) {
            //    throw new AplException.NotValidException("Invalid attachment version " + appendage.getVersion());
            //}
            AppendixValidator<AbstractAppendix> validator = validatorRegistry.getValidatorFor(appendage);
            if (validatingAtFinish) {
                if (validator != null) {
                    validator.validateAtFinish(transaction, appendage, blockchain.getHeight());
                } else {
                    appendage.validateAtFinish(transaction, blockchain.getHeight());
                }
            } else {
                if (validator != null) {
                    validator.validate(transaction, appendage, blockchain.getHeight());
                } else {
                    appendage.validate(transaction, blockchain.getHeight());
                }
            }
        }
        int fullSize = transaction.getFullSize();
        if (fullSize > blockchainConfig.getCurrentConfig().getMaxPayloadLength()) {
            throw new AplException.NotValidException("Transaction size " + fullSize + " exceeds maximum payload size");
        }
        int blockchainHeight = blockchain.getHeight();
        if (!validatingAtFinish) {
            long minimumFeeATM = feeCalculator.getMinimumFeeATM(transaction, blockchainHeight);
            if (feeATM < minimumFeeATM) {
                throw new AplException.NotCurrentlyValidException(String.format("Transaction fee %f %s less than minimum fee %f %s at height %d",
                    ((double) feeATM) / Constants.ONE_APL, blockchainConfig.getCoinSymbol(), ((double) minimumFeeATM) / Constants.ONE_APL, blockchainConfig.getCoinSymbol(),
                    blockchainHeight));
            }
            long ecBlockId = transaction.getECBlockId();
            int ecBlockHeight = transaction.getECBlockHeight();
            if (ecBlockId != 0) {
                if (blockchainHeight < ecBlockHeight) {
                    throw new AplException.NotCurrentlyValidException("ecBlockHeight " + ecBlockHeight
                        + " exceeds blockchain height " + blockchainHeight);
                }
                if (blockchain.getBlockIdAtHeight(ecBlockHeight) != ecBlockId) {
                    throw new AplException.NotCurrentlyValidException("ecBlockHeight " + ecBlockHeight
                        + " does not match ecBlockId " + Long.toUnsignedString(ecBlockId)
                        + ", transaction was generated on a fork");
                }
            }
            accountControlPhasingService.checkTransaction(transaction);
        }
    }

    public int getActualTransactionVersion() {
        return transactionVersionValidator.getActualVersion();
    }

    public boolean isValidVersion(int transactionVersion) {
        return transactionVersionValidator.isValidVersion(transactionVersion);
    }

    public void checkVersion(int transactionVersion) {
        transactionVersionValidator.checkVersion(transactionVersion);
    }

    public boolean checkSignature(Transaction transaction) {
        if (transaction.hasValidSignature()) {
            return true;
        }
        Account sender = accountService.getAccount(transaction.getSenderId());
        if (sender == null) {
            log.debug("Sender account not found, senderId={}", transaction.getSenderId());
        }
        @ParentChildSpecific(ParentMarker.MULTI_SIGNATURE)
        Credential signatureCredential;
        SignatureVerifier signatureVerifier = SignatureToolFactory.selectValidator(transaction.getVersion()).orElseThrow(UnsupportedTransactionVersion::new);
        if (log.isTraceEnabled()) {
            log.trace("#MULTI_SIG# verify signature validator class={}", signatureVerifier.getClass().getName());
        }
        if (sender != null && sender.isChild()) {
            //multi-signature
            if (transaction.getVersion() < 2) {
                log.error("Inconsistent transaction fields, the value of the sender property 'parent' doesn't match the transaction version.");
                return false;
            }
            signatureCredential = new MultiSigCredential(2,
                accountService.getPublicKeyByteArray(sender.getParentId()),
                transaction.getSenderPublicKey()
            );
        } else {
            //only one signer
            if (transaction.getVersion() < 2) {
                signatureCredential = new SignatureCredential(transaction.getSenderPublicKey());
            } else {
                signatureCredential = new MultiSigCredential(transaction.getSenderPublicKey());
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("#MULTI_SIG# verify credential={}", signatureCredential);
        }
        if (!signatureCredential.validateCredential(keyValidator)) {
            return false;
        }

        if (transaction.getSignature() != null && transaction.getSignature().isVerified()) {
            return true;
        } else {
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# verify signature={} publicKey={} document={}",
                    Convert.toHexString(transaction.getSignature().bytes()),
                    signatureCredential,
                    Convert.toHexString(transaction.getUnsignedBytes()));
            }

            boolean verifiedOk = signatureVerifier.verify(
                transaction.getUnsignedBytes(), transaction.getSignature(), signatureCredential
            );
            if (verifiedOk) {
                transaction.withValidSignature();
            }
            return verifiedOk;
        }
    }


    public boolean verifySignature(Transaction transaction) {
        return checkSignature(transaction) && accountPublicKeyService.setOrVerifyPublicKey(transaction.getSenderId(), transaction.getSenderPublicKey());
    }

    private static class PublicKeyValidator implements KeyValidator {
        private final AccountPublicKeyService accountPublicKeyService;

        public PublicKeyValidator(AccountPublicKeyService accountPublicKeyService) {
            this.accountPublicKeyService = accountPublicKeyService;
        }

        @Override
        public boolean validate(byte[] publicKey) {
            if (log.isTraceEnabled()) {
                log.trace("#MULTI_SIG# public key validation account={}", Convert2.rsAccount(AccountService.getId(publicKey)));
            }
            if (!accountPublicKeyService.setOrVerifyPublicKey(AccountService.getId(publicKey), publicKey)) {
                log.error("Public Key Verification failed: pk={}", Convert.toHexString(publicKey));
                return false;
            }
            return true;
        }
    }
}
