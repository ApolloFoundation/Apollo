package com.apollocurrency.aplwallet.apl.core.rest;

import com.apollocurrency.aplwallet.api.v2.model.TransactionCreationResponse;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionFeatureNotEnabledException;
import com.apollocurrency.aplwallet.apl.core.exception.AplTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSigner;
import com.apollocurrency.aplwallet.apl.core.transaction.FeeCalculator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionType;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypeFactory;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionWrapperHelper;
import com.apollocurrency.aplwallet.apl.core.transaction.common.TxBContext;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunablePlainMessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PublicKeyAnnouncementAppendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.ApiErrors;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.exception.RestParameterException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.apollocurrency.aplwallet.apl.util.io.PayloadResult;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.apollocurrency.aplwallet.apl.core.transaction.TransactionVersionValidator.DEFAULT_VERSION;

@Singleton
public class TransactionCreator {
    private final TransactionValidator validator;
    private final PropertiesHolder propertiesHolder;
    private final TimeService timeService;
    private final FeeCalculator feeCalculator;
    private final Blockchain blockchain;
    private final TransactionProcessor processor;
    private final TransactionTypeFactory typeFactory;
    private final TransactionBuilderFactory transactionBuilderFactory;
    private final TransactionSigner signerService;
    private final BlockchainConfig blockchainConfig;
    private final TxBContext txBContext;

    @Inject
    public TransactionCreator(TransactionValidator validator, PropertiesHolder propertiesHolder, TimeService timeService, FeeCalculator feeCalculator, Blockchain blockchain, TransactionProcessor processor, TransactionTypeFactory typeFactory, TransactionBuilderFactory transactionBuilderFactory, TransactionSigner signer, BlockchainConfig blockchainConfig) {
        this.validator = validator;
        this.propertiesHolder = propertiesHolder;
        this.timeService = timeService;
        this.feeCalculator = feeCalculator;
        this.blockchain = blockchain;
        this.processor = processor;
        this.typeFactory = typeFactory;
        this.transactionBuilderFactory = transactionBuilderFactory;
        this.signerService = signer;
        this.blockchainConfig = blockchainConfig;
        txBContext = TxBContext.newInstance(this.blockchainConfig.getChain());
    }

    public TransactionCreationData createTransaction(CreateTransactionRequest txRequest) {
        int version = txRequest.getVersion() != null ? txRequest.getVersion() : DEFAULT_VERSION;

        TransactionCreationData tcd = new TransactionCreationData();
        EncryptedMessageAppendix encryptedMessage = null;
        PrunableEncryptedMessageAppendix prunableEncryptedMessage = null;
        TransactionTypes.TransactionTypeSpec typeSpec = txRequest.getAttachment().getTransactionTypeSpec();
        TransactionType transactionType = typeFactory.findTransactionType(typeSpec.getType(), typeSpec.getSubtype());
        if (transactionType.canHaveRecipient() && txRequest.getRecipientId() != 0) {
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
        if (txRequest.getKeySeed() == null && txRequest.getSecretPhrase() != null) {
            txRequest.setKeySeed(Crypto.getKeySeed(txRequest.getSecretPhrase()));
        }

        if (txRequest.getKeySeed() != null) {
            txRequest.setPublicKey(Crypto.getPublicKey(txRequest.getKeySeed()));
        }

        if (txRequest.getKeySeed() == null && txRequest.getPublicKey() == null && txRequest.getCredential() == null) {
            tcd.setErrorType(TransactionCreationData.ErrorType.MISSING_SECRET_PHRASE);
            return tcd;
        }

        if (txRequest.getDeadlineValue() == null) {
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
            Transaction.Builder builder;
            if(version < 3) {
                builder = transactionBuilderFactory.newUnsignedTransactionBuilder(version, txRequest.getPublicKey(),
                    txRequest.getAmountATM(), txRequest.getFeeATM(),
                    deadline, txRequest.getAttachment(), timestamp);
            }else {
                throw new IllegalStateException("Unsupported transaction version.");
            }
            builder.referencedTransactionFullHash(txRequest.getReferencedTransactionFullHash());
            if (transactionType.canHaveRecipient()) {
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
            } else {
                EcBlockData ecBlock = blockchain.getECBlock(timestamp);
                builder.ecBlockData(ecBlock);
            }

            //build unsigned transaction
            transaction = builder.build();

            if (txRequest.getFeeATM() <= 0 || (propertiesHolder.correctInvalidFees() && txRequest.getKeySeed() == null)) {
                int effectiveHeight = blockchain.getHeight();
                @TransactionFee(FeeMarker.CALCULATOR)
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

            //Sign transaction
            if (version < 2) { //tx v1
                if (txRequest.getKeySeed() != null) {
                    signerService.sign(transaction, txRequest.getKeySeed());
                }
            } else {//tx version >= 2
                if (txRequest.getCredential() != null) {
                    signerService.sign(transaction, txRequest.getCredential());
                }
            }

            if (txRequest.isBroadcast() && transaction.getSignature() != null) {
                processor.broadcast(transaction);
            } else if (txRequest.isValidate()) {
                validator.validateFully(transaction);
            }
            tcd.setTx(transaction);
        } catch (AplTransactionFeatureNotEnabledException e) {
            tcd.setErrorType(TransactionCreationData.ErrorType.FEATURE_NOT_AVAILABLE);
            tcd.setError(e.getMessage());
        } catch (AplException.ValidationException | AplTransactionValidationException e) {
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
                default:
                    throw new RuntimeException("For " + transaction.getErrorType() + " no error throwing mappings was found");
            }
        }
        return transaction.getTx();
    }

    public TransactionCreationResponse createApiV2Transaction(CreateTransactionRequest request) {
        Transaction tx = createTransactionThrowingException(request);
        TransactionCreationResponse response = new TransactionCreationResponse();
        boolean signed = tx.getSignature() != null;
        response.setBroadcasted(request.isBroadcast() && signed);
        if (signed) {
            response.setId(tx.getStringId());
            response.setSignature(tx.getSignature().getHexString());
            response.setFullHash(tx.getFullHashString());
            response.setTransactionBytes(Convert.toHexString(getSignedTxBytes(tx)));
        }
        response.setUnsignedTransactionBytes(Convert.toHexString(getUnsignedTxBytes(tx)));
        return response;
    }

    private byte[] getUnsignedTxBytes(Transaction tx) {
        PayloadResult unsignedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(tx.getVersion())
            .serialize(TransactionWrapperHelper.createUnsignedTransaction(tx) , unsignedTxBytes);
        return unsignedTxBytes.array();
    }

    private byte[] getSignedTxBytes(Transaction tx) {
        PayloadResult signedTxBytes = PayloadResult.createLittleEndianByteArrayResult();
        txBContext.createSerializer(tx.getVersion()).serialize(tx, signedTxBytes);
        return signedTxBytes.array();
    }

    @Data
    public static class TransactionCreationData {
        Transaction tx;
        String error = "";
        ErrorType errorType;

        public boolean hasError() {
            return errorType != null;
        }

        public enum ErrorType {
            INCORRECT_DEADLINE, MISSING_DEADLINE, MISSING_SECRET_PHRASE,
            INCORRECT_EC_BLOCK, FEATURE_NOT_AVAILABLE, NOT_ENOUGH_APL, VALIDATION_FAILED
        }
    }
}
