package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.TimeService;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.app.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.rest.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionCreator {
    private final TransactionValidator validator;
    private final PropertiesHolder propertiesHolder;
    private final TimeService timeService;
    private final FeeCalculator feeCalculator;
    private final Blockchain blockchain;
    private final TransactionProcessor processor;

    @Inject
    public TransactionCreator(TransactionValidator validator, PropertiesHolder propertiesHolder, TimeService timeService, FeeCalculator feeCalculator, Blockchain blockchain, TransactionProcessor processor) {
        this.validator = validator;
        this.propertiesHolder = propertiesHolder;
        this.timeService = timeService;
        this.feeCalculator = feeCalculator;
        this.blockchain = blockchain;
        this.processor = processor;
    }

    public TransactionCreationData createTransaction(CreateTransactionRequest txRequest) {
        TransactionCreationData tcd = new TransactionCreationData();
        EncryptedMessageAppendix encryptedMessage = null;
        PrunableEncryptedMessageAppendix prunableEncryptedMessage = null;

        if (txRequest.getAttachment().getTransactionType().canHaveRecipient() && txRequest.getRecipientId() != 0) {
            if (txRequest.isEncryptedMessageIsPrunable()) {
                prunableEncryptedMessage = (PrunableEncryptedMessageAppendix) txRequest.getAppendix();
            } else {
                encryptedMessage = (EncryptedMessageAppendix) txRequest.getAppendix();
            }
        }

        MessageAppendix message = txRequest.isMessageIsPrunable() ? null : (MessageAppendix) txRequest.getMessage();
        PrunablePlainMessageAppendix prunablePlainMessage = txRequest.isMessageIsPrunable() ? (PrunablePlainMessageAppendix) txRequest.getMessage() : null;

        PublicKeyAnnouncementAppendix publicKeyAnnouncement = null;
        if (txRequest.getRecipientPublicKey() != null) {
            publicKeyAnnouncement = new PublicKeyAnnouncementAppendix(Convert.parseHexString(txRequest.getRecipientPublicKey()));
        }
        if (txRequest.getKeySeed() != null) {
            txRequest.setPublicKey(Crypto.getPublicKey(txRequest.getKeySeed()));
        }

        if (txRequest.getKeySeed() == null && txRequest.getPublicKey() == null) {
            tcd.setErrorType(TransactionCreationData.ErrorType.MISSING_SECRET_PHRASE);
            return tcd;
        } else if (txRequest.getDeadlineValue() == null) {
            tcd.setErrorType(TransactionCreationData.ErrorType.MISSING_DEADLINE);
            return tcd;
        }

        short deadline;
        try {
            deadline = Short.parseShort(txRequest.getDeadlineValue());
            if (deadline < 1) {
                tcd.setErrorType(TransactionCreationData.ErrorType.INCORRECT_DEADLINE);
                return tcd;
            }
        } catch (NumberFormatException e) {
            tcd.setErrorType(TransactionCreationData.ErrorType.INCORRECT_DEADLINE);
            return tcd;
        }

        if (txRequest.getEcBlockId() != 0 && txRequest.getEcBlockId() != blockchain.getBlockIdAtHeight(txRequest.getEcBlockHeight())) {
            tcd.setErrorType(TransactionCreationData.ErrorType.INCORRECT_EC_BLOCK);
            return tcd;
        }
        if (txRequest.getEcBlockId() == 0 && txRequest.getEcBlockHeight() > 0) {
            txRequest.setEcBlockId(blockchain.getBlockIdAtHeight(txRequest.getEcBlockHeight()));
        }

        // shouldn't try to get publicKey from senderAccount as it may have not been set yet
        int timestamp = txRequest.getTimestamp() != 0 ? txRequest.getTimestamp() : timeService.getEpochTime();
        Transaction transaction;
        try {
            Transaction.Builder builder = Transaction.newTransactionBuilder(txRequest.getPublicKey(), txRequest.getAmountATM(), txRequest.getFeeATM(),
                deadline, txRequest.getAttachment(), timestamp).referencedTransactionFullHash(txRequest.getReferencedTransactionFullHash());
            if (txRequest.getAttachment().getTransactionType().canHaveRecipient()) {
                builder.recipientId(txRequest.getRecipientId());
            }
            builder.appendix(encryptedMessage);
            builder.appendix(message);
            builder.appendix(publicKeyAnnouncement);
            builder.appendix(txRequest.getEncryptToSelfMessage());
            builder.appendix(txRequest.getPhasing());
            builder.appendix(prunablePlainMessage);
            builder.appendix(prunableEncryptedMessage);
            if (txRequest.getEcBlockId() != 0) {
                builder.ecBlockId(txRequest.getEcBlockId());
                builder.ecBlockHeight(txRequest.getEcBlockHeight());
            }
            transaction = builder.build(txRequest.getKeySeed());
            if (txRequest.getFeeATM() <= 0 || (propertiesHolder.correctInvalidFees() && txRequest.getKeySeed() == null)) {
                int effectiveHeight = blockchain.getHeight();
                long minFee = feeCalculator.getMinimumFeeATM(transaction, effectiveHeight);
                txRequest.setFeeATM(Math.max(minFee, txRequest.getFeeATM()));
                transaction.setFeeATM(txRequest.getFeeATM());
            }

            try {
                if (Math.addExact(txRequest.getAmountATM(), transaction.getFeeATM()) > txRequest.getSenderAccount().getUnconfirmedBalanceATM()) {
                    tcd.setErrorType(TransactionCreationData.ErrorType.NOT_ENOUGH_APL);
                    return tcd;
                }
            } catch (ArithmeticException e) {
                tcd.setErrorType(TransactionCreationData.ErrorType.NOT_ENOUGH_APL);
                return tcd;
            }

            if (txRequest.isBroadcast()) {
                processor.broadcast(transaction);
            } else if (txRequest.isValidate()) {
                validator.validate(transaction);
            }
            tcd.setTx(transaction);
        } catch (AplException.NotYetEnabledException e) {
            tcd.setErrorType(TransactionCreationData.ErrorType.FEATURE_NOT_AVAILABLE);
        } catch (AplException.InsufficientBalanceException e) {
            tcd.setErrorType(TransactionCreationData.ErrorType.INSUFFICIENT_BALANCE_ON_APPLY_UNCONFIRMED);
        } catch (AplException.ValidationException e) {
            tcd.setErrorType(TransactionCreationData.ErrorType.VALIDATION_FAILED);
            tcd.setError(e.getMessage());
        }
        return tcd;
    }

    public Transaction createTransactionThrowingException(CreateTransactionRequest request) {
        TransactionCreationData transaction = createTransaction(request);
        if (transaction.hasError()) {
            switch (transaction.getErrorType()) {
                case NOT_ENOUGH_APL:
                    throw new RestParameterException(ApiErrors.NOT_ENOUGH_FUNDS, "APL");
                case MISSING_DEADLINE:
                    throw new RestParameterException(ApiErrors.MISSING_PARAM, "deadline");
                case INCORRECT_DEADLINE:
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM_VALUE, "deadline");
                case INCORRECT_EC_BLOCK:
                    throw new RestParameterException(ApiErrors.INCORRECT_PARAM, "ecBlockId", "ecBlockId does not match the block id at ecBlockHeight");
                case VALIDATION_FAILED:
                    throw new RestParameterException(ApiErrors.TX_VALIDATION_FAILED, transaction.getError());
                case FEATURE_NOT_AVAILABLE:
                    throw new RestParameterException(ApiErrors.FEATURE_NOT_ENABLED);
                case MISSING_SECRET_PHRASE:
                    throw new RestParameterException(ApiErrors.MISSING_PARAM_LIST, "secretPhrase,passphrase");
                case INSUFFICIENT_BALANCE_ON_APPLY_UNCONFIRMED:
                    throw new RestParameterException(ApiErrors.TX_VALIDATION_FAILED, " not enough funds (APL,ASSET,CURRENCY)");
                default:
                    throw new RuntimeException("For " + transaction.getErrorType() + " no error throwing mappings was found");
            }
        }
        return transaction.getTx();
    }

    @Data
    static class TransactionCreationData {
        Transaction tx;
        String error = "";
        ErrorType errorType;

        public boolean hasError() {
            return errorType != null;
        }

        public enum ErrorType {
            INCORRECT_DEADLINE, MISSING_DEADLINE, MISSING_SECRET_PHRASE, INCORRECT_EC_BLOCK, FEATURE_NOT_AVAILABLE, NOT_ENOUGH_APL, INSUFFICIENT_BALANCE_ON_APPLY_UNCONFIRMED, VALIDATION_FAILED
        }
    }
}
