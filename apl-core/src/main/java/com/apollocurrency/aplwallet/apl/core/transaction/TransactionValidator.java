/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.antifraud.AntifraudValidator;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
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
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.annotation.ParentChildSpecific;
import com.apollocurrency.aplwallet.apl.util.annotation.ParentMarker;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import com.apollocurrency.aplwallet.apl.util.io.Result;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

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
    private final AntifraudValidator antifraudValidator;

    private final TxBContext txBContext;

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
        this.antifraudValidator = new AntifraudValidator();
        this.txBContext = TxBContext.newInstance(blockchainConfig.getChain());
    }


    public int getFinishValidationHeight(Transaction transaction, Attachment attachment) {
        return attachment.isPhased(transaction) ? transaction.getPhasing().getFinishHeight() - 1 : blockchain.getHeight();
    }

    /**
     * Validate transaction lightly without state fetch using its own data and node's in-memory info
     * @param transaction transaction to validate
     * @throws AplUnacceptableTransactionValidationException if transaction's verification failed and transaction should be cared as fully invalid
     */
    public void validateLightly(Transaction transaction) {
        validateLightlyWithoutAppendages(transaction);
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            AppendixValidator<AbstractAppendix> validatorFor = validatorRegistry.getValidatorFor(appendage);
            verifyAppendageVersion(transaction, appendage);
            doAppendixLightValidationRethrowing(transaction, appendage, validatorFor);
        }
    }

    /**
     * Validate transaction's signature, sender's fee payability and transaction's data validity without state fetch
     * @param transaction transaction to validate
     * @throws AplUnacceptableTransactionValidationException when transaction does not pass validation
     */
    public void validateSufficiently(Transaction transaction) {
        validateLightly(transaction);
        validateSignatureWithTxFee(transaction);
    }

    /**
     * Fully validate transaction against the current blockchain state,
     * Note the signature verification is performed by separate method {@link TransactionValidator#verifySignature(Transaction)}
     * @param transaction transaction to validate
     * @throws AplAcceptableTransactionValidationException when transaction's appendix/attachment verification failed and transaction may be accepted
     * @throws AplUnacceptableTransactionValidationException when transaction general verification failed and transaction is not valid at all
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplTransactionFeatureNotEnabledException when transaction is not allowed yet
     */
    public void validateFully(Transaction transaction) {
       validateLightlyWithoutAppendages(transaction);

        if (!antifraudValidator.validate(blockchain.getHeight(), blockchainConfig.getChain().getChainId(), transaction.getSenderId(),
            transaction.getRecipientId())) {
            throw new AplUnacceptableTransactionValidationException("Incorrect Passphrase", transaction);
        }

        Account sender = accountService.getAccount(transaction.getSenderId());
        validateChildAccountsSpecific(transaction, sender);

        boolean validatingAtFinish = transaction.getPhasing() != null && transaction.getSignature() != null && phasingPollService.getPoll(transaction.getId()) != null;
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion()).serialize(transaction, byteArrayTx);
        int fullSize = TransactionUtils.calculateFullSize(transaction, byteArrayTx.size());
        if (fullSize > blockchainConfig.getCurrentConfig().getMaxPayloadLength()) {
            throw new AplUnacceptableTransactionValidationException("Transaction size " + fullSize + " exceeds maximum payload size", transaction);
        }
        int blockchainHeight = blockchain.getHeight();
        if (!validatingAtFinish) {
            validateFee(sender, transaction, blockchainHeight);
            long ecBlockId = transaction.getECBlockId();
            int ecBlockHeight = transaction.getECBlockHeight();
            if (ecBlockId != 0) {
                if (blockchainHeight < ecBlockHeight) {
                    throw new AplUnacceptableTransactionValidationException("ecBlockHeight " + ecBlockHeight
                        + " exceeds blockchain height " + blockchainHeight, transaction);
                }
                if (blockchain.getBlockIdAtHeight(ecBlockHeight) != ecBlockId) {
                    throw new AplUnacceptableTransactionValidationException("ecBlockHeight " + ecBlockHeight
                        + " does not match ecBlockId " + Long.toUnsignedString(ecBlockId)
                        + ", transaction was generated on a fork", transaction);
                }
            }
            try {
                accountControlPhasingService.checkTransaction(transaction);
            } catch (AplException.NotCurrentlyValidException e) {
                throw new AplUnacceptableTransactionValidationException(e.getMessage(), e, transaction);
            }
        }
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            verifyAppendageVersion(transaction, appendage);
            prunableService.loadPrunable(transaction, appendage, false);
            AppendixValidator<AbstractAppendix> validator = validatorRegistry.getValidatorFor(appendage);
            if (validatingAtFinish) {
                validateAtFinishRethrowing(transaction, appendage, validator);
            } else {
                doAppendixLightValidationRethrowing(transaction, appendage, validator);
                doAppendixStateDependentValidationRethrowing(transaction, appendage, validator);
            }
        }
    }

    public boolean isValidVersion(int txVersion) {
        return transactionVersionValidator.isValidVersion(txVersion);
    }

    public void validateSignatureWithTxFee(Transaction transaction) {
        Account sender = accountService.getAccount(transaction.getSenderId());
        if (sender == null) {
            throw new AplUnacceptableTransactionValidationException("Account " + Long.toUnsignedString(transaction.getSenderId()) + " does not exist yet", transaction);
        }
        int height = blockchain.getHeight();
        validateFee(sender, transaction, height);
        checkSignatureThrowingEx(transaction, sender);
    }

    public boolean checkSignature(Transaction transaction) {
        return checkSignature(accountService.getAccount(transaction.getSenderId()), transaction);
    }

    public boolean checkSignature(Account sender, Transaction transaction) {
        if (transaction.hasValidSignature()) {
            return true;
        }
        if (transaction.getSignature() == null) {
            return false;
        }
        if (sender == null) {
            log.debug("Sender account is null, senderId={}, tx={}", Long.toUnsignedString(transaction.getSenderId()),
                transaction.getStringId());
        }
        @ParentChildSpecific(ParentMarker.MULTI_SIGNATURE)
        Credential signatureCredential;
        Optional<SignatureVerifier> signatureVerifierOptional = SignatureToolFactory.selectValidator(transaction.getVersion());
        if (signatureVerifierOptional.isEmpty()) {
            log.error("Unsupported version: '{}' of the transaction: '{}'", transaction.getVersion(), transaction.getStringId());
            return false;
        }
        SignatureVerifier signatureVerifier = signatureVerifierOptional.get();
        log.trace("#MULTI_SIG# verify signature validator class={}", signatureVerifier.getClass().getName());
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
        log.trace("#MULTI_SIG# verify credential={}", signatureCredential);
        if (!signatureCredential.validateCredential(keyValidator)) {
            log.error("#MULTI_SIG# Credential verification failed, credential={}", signatureCredential);
            return false;
        }
        Result byteArrayTx = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(transaction.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(transaction), byteArrayTx);

        log.trace("#MULTI_SIG# verify signature={} publicKey={} document={}",
            Convert.toHexString(transaction.getSignature().bytes()),
            signatureCredential,
            Convert.toHexString(byteArrayTx.array()));

        return signatureVerifier.verify(byteArrayTx.array(), transaction.getSignature(), signatureCredential);
    }

    public boolean verifySignature(Transaction transaction) {
        return checkSignature(transaction)
            && accountPublicKeyService.setOrVerifyPublicKey(transaction.getSenderId(), transaction.getSenderPublicKey());
    }

    private void doAppendixLightValidationRethrowing(Transaction transaction, AbstractAppendix appendage, AppendixValidator<AbstractAppendix> validatorFor) {
        try {
            doAppendixLightValidation(validatorFor, transaction, appendage);
        } catch (AplException.ValidationException e) {
            throw new AplUnacceptableTransactionValidationException(e.getMessage(), e, transaction);
        }
    }

    private void checkSignatureThrowingEx(Transaction transaction, Account account) {
        if (!checkSignature(account, transaction)) {
            throw new AplUnacceptableTransactionValidationException("Invalid signature for transaction " + transaction.getStringId(), transaction);
        }
    }

    private void validateAtFinish(AppendixValidator<AbstractAppendix> validator, Transaction transaction, AbstractAppendix appendix) throws AplException.ValidationException {
        if (validator != null) {
            validator.validateAtFinish(transaction, appendix, blockchain.getHeight());
        } else {
            appendix.validateAtFinish(transaction, blockchain.getHeight());
        }
    }

    private void doAppendixStateDependentValidationRethrowing(Transaction transaction, AbstractAppendix appendage, AppendixValidator<AbstractAppendix> validator) {
        try {
            doAppendixStateDependentValidation(validator, transaction, appendage);
        } catch (AplException.ValidationException e) {
            throw new AplAcceptableTransactionValidationException(e.getMessage(), e, transaction);
        }
    }

    private void validateAtFinishRethrowing(Transaction transaction, AbstractAppendix appendage, AppendixValidator<AbstractAppendix> validator) {
        try {
            validateAtFinish(validator, transaction, appendage);
        } catch (AplException.ValidationException e) {
            throw new AplUnacceptableTransactionValidationException(e.getMessage(), transaction);
        }
    }

    /**
     * Performs sender's transaction fee payability verification
     * @param account sender's account, may be null
     * @param transaction transaction to validate fee amount
     * @param blockchainHeight height of the blockchain on which perform validation
     * @throws AplUnacceptableTransactionValidationException when sender's has not enough funds to pay transaction fee or sender's account does not exist
     */
    private void validateFee(Account account, Transaction transaction, int blockchainHeight) {
        validateFeeSufficiency(transaction, blockchainHeight);
        long feeATM = transaction.getFeeATM();
        if (transaction.referencedTransactionFullHash() != null) {
            feeATM = Math.addExact(feeATM, blockchainConfig.getUnconfirmedPoolDepositAtm());
        }
        if (account == null) {
            throw new AplUnacceptableTransactionValidationException("Sender's account '" +
                Long.toUnsignedString(transaction.getSenderId()) + "' does not exist yet", transaction);
        }
        if (account.getUnconfirmedBalanceATM() < feeATM) {
            throw new AplUnacceptableTransactionValidationException("Account '" + Long.toUnsignedString(transaction.getSenderId())
                +"' balance " + account.getUnconfirmedBalanceATM() + " is not enough to pay tx fee " + feeATM, transaction);
        }
    }

    /**
     * Verify that fee set for the transaction is enough at given blockchain height
     * @param transaction transaction to validate fee amount
     * @param blockchainHeight height of the blockchain on which perform validation
     * @throws AplUnacceptableTransactionValidationException when transaction's fee is not enough
     */
    private void validateFeeSufficiency(Transaction transaction, int blockchainHeight) {
        long feeATM = transaction.getFeeATM();
        long minimumFeeATM = feeCalculator.getMinimumFeeATM(transaction, blockchainHeight);
        if (feeATM < minimumFeeATM) {
            throw new AplUnacceptableTransactionValidationException(String.format("Transaction fee %f %s less than minimum fee %f %s at height %d",
                ((double) feeATM) / blockchainConfig.getOneAPL(), blockchainConfig.getCoinSymbol(), ((double) minimumFeeATM) / blockchainConfig.getOneAPL(), blockchainConfig.getCoinSymbol(),
                blockchainHeight), transaction);
        }
    }

    /**
     * Perform light transaction validation by its own data without state fetch
     * @param transaction transaction to validate
     * @throws AplUnacceptableTransactionValidationException when transaction does not pass validation
     */
    private void validateLightlyWithoutAppendages(Transaction transaction) {
        if (!transactionVersionValidator.isValidVersion(transaction)) {
            throw new AplUnacceptableTransactionValidationException("Unsupported transaction version " + transaction.getVersion() + " at height " + blockchain.getHeight(), transaction);
        }
        long maxBalanceAtm = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        short deadline = transaction.getDeadline();
        long feeATM = transaction.getFeeATM();
        long amountATM = transaction.getAmountATM();
        TransactionType type = transaction.getType();
        TransactionTypes.TransactionTypeSpec typeSpec = type.getSpec();
        if (typeSpec == null
            || (transaction.getTimestamp() == 0 ? (deadline != 0 || feeATM != 0) : (deadline < 1 || feeATM < 0))
            || feeATM > maxBalanceAtm
            || amountATM < 0
            || amountATM > maxBalanceAtm) {
            throw new AplUnacceptableTransactionValidationException("Invalid transaction parameters: type: " + type + ", timestamp: " + transaction.getTimestamp()
                + ", deadline: " + deadline + ", fee: " + feeATM + ", amount: " + amountATM, transaction);
        }
        byte[] referencedTransactionFullHash = Convert.parseHexString(transaction.getReferencedTransactionFullHash());

        if (referencedTransactionFullHash != null && referencedTransactionFullHash.length != 32) {
            throw new AplUnacceptableTransactionValidationException("Invalid referenced transaction full hash " + Convert.toHexString(referencedTransactionFullHash), transaction);
        }
        Attachment attachment = transaction.getAttachment();

        if (attachment == null || typeSpec != attachment.getTransactionTypeSpec()) {
            throw new AplUnacceptableTransactionValidationException("Invalid attachment " + attachment + " for transaction of type " + type, transaction);
        }

        long recipientId = transaction.getRecipientId();
        if (!type.canHaveRecipient() && (recipientId != 0 || amountATM != 0)) {
            throw new AplUnacceptableTransactionValidationException("Transactions of this type must have recipient == 0, amount == 0", transaction);
        }

        if (type.mustHaveRecipient() && recipientId == 0) {
            throw new AplUnacceptableTransactionValidationException("Transactions of this type must have a valid recipient", transaction);
        }
    }

    private void doAppendixStateDependentValidation(AppendixValidator<AbstractAppendix> validator, Transaction transaction, AbstractAppendix appendage) throws AplException.ValidationException {
        if (validator != null) {
            validator.validateStateDependent(transaction, appendage, blockchain.getHeight());
        } else {
            appendage.performStateDependentValidation(transaction, blockchain.getHeight());
        }
    }

    private void doAppendixLightValidation(AppendixValidator<AbstractAppendix> validator, Transaction transaction, AbstractAppendix appendage) throws AplException.ValidationException {
        if (validator != null) {
            validator.validateStateIndependent(transaction, appendage, blockchain.getHeight());
        } else {
            appendage.performStateIndependentValidation(transaction, blockchain.getHeight());
        }
    }

    private void validateChildAccountsSpecific(Transaction transaction, Account sender) {
        if (sender != null && sender.isChild()) {
            Account recipient = accountService.getAccount(transaction.getRecipientId());
            if (recipient == null) {
                throw new AplUnacceptableTransactionValidationException("Recipient's account " +
                    "'" + Long.toUnsignedString(transaction.getRecipientId()) + "' does not exist yet", transaction);
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
                    throw new AplUnacceptableTransactionValidationException("Unsupported value '" +
                        sender.getAddrScope().name() +
                        "' for sender address scope;" +
                        "sender.Id=" + Long.toUnsignedString(sender.getId()), transaction);
            }
            if (!rc) {
                throw new AplUnacceptableTransactionValidationException("The parent account for sender and recipient must be the same;" +
                    "sender.parentId=" + Long.toUnsignedString(sender.getParentId()) + ", recipient.parentId=" + Long.toUnsignedString(recipient.getParentId()), transaction);
            }
        }
    }

    private void verifyAppendageVersion(Transaction transaction, AbstractAppendix appendage) {
        if (!appendage.verifyVersion()) {
            throw new AplUnacceptableTransactionValidationException(appendage.getAppendixName() + " appendage version '" + appendage.getVersion() + "' is not supported", transaction);
        }
    }

    private static class PublicKeyValidator implements KeyValidator {
        private final AccountPublicKeyService accountPublicKeyService;

        public PublicKeyValidator(AccountPublicKeyService accountPublicKeyService) {
            this.accountPublicKeyService = accountPublicKeyService;
        }

        @Override
        public boolean validate(byte[] publicKey) {
            long accountId = AccountService.getId(publicKey);
            log.trace("#MULTI_SIG# public key validation account={}", Long.toUnsignedString(accountId));
            if (!accountPublicKeyService.setOrVerifyPublicKey(accountId, publicKey)) {
                log.error("Public Key Verification failed: pk={}", Convert.toHexString(publicKey));
                return false;
            }
            return true;
        }
    }
}
