/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.antifraud.AntifraudValidator;
import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Attachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.crypto.Crypto;
import com.apollocurrency.aplwallet.apl.util.Constants;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class TransactionValidator {
    private final BlockchainConfig blockchainConfig;
    private final PhasingPollService phasingPollService;
    private final Blockchain blockchain;
    private final FeeCalculator feeCalculator;
    private final AccountPublicKeyService accountPublicKeyService;
    private final AccountControlPhasingService accountControlPhasingService;

    @Inject
    public TransactionValidator(BlockchainConfig blockchainConfig, PhasingPollService phasingPollService,
                                Blockchain blockchain, FeeCalculator feeCalculator,
                                AccountPublicKeyService accountPublicKeyService, AccountControlPhasingService accountControlPhasingService) {
        this.blockchainConfig = blockchainConfig;
        this.phasingPollService = phasingPollService;
        this.blockchain = blockchain;
        this.feeCalculator = feeCalculator;
        this.accountPublicKeyService = accountPublicKeyService;
        this.accountControlPhasingService = accountControlPhasingService;
    }

    public void validate(Transaction transaction) throws AplException.ValidationException {
        long maxBalanceAtm = blockchainConfig.getCurrentConfig().getMaxBalanceATM();
        short deadline = transaction.getDeadline();
        long feeATM = transaction.getFeeATM();
        long amountATM = transaction.getAmountATM();
        TransactionType type = transaction.getType();
        TransactionTypes.TransactionTypeSpec typeSpec = type.getSpec();
        if (transaction.getTimestamp() == 0 ? (deadline != 0 || feeATM != 0) : (deadline < 1 || feeATM <= 0)
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

        if (attachment == null || typeSpec != attachment.getTransactionType()) {
            throw new AplException.NotValidException("Invalid attachment " + attachment + " for transaction of type " + type);
        }
        long recipientId = transaction.getRecipientId();
        if (!type.canHaveRecipient()) {
            if (recipientId != 0 || amountATM != 0) {
                throw new AplException.NotValidException("Transactions of this type must have recipient == 0, amount == 0");
            }
        }

        if (type.mustHaveRecipient()) {
            if (recipientId == 0) {
                throw new AplException.NotValidException("Transactions of this type must have a valid recipient");
            }
        }

        if (!AntifraudValidator.validate(blockchain.getHeight(), transaction.getSenderId(),
            transaction.getRecipientId())) throw new AplException.NotValidException("Incorrect Passphrase");


        boolean validatingAtFinish = transaction.getPhasing() != null && transaction.getSignature() != null && phasingPollService.getPoll(transaction.getId()) != null;
        for (AbstractAppendix appendage : transaction.getAppendages()) {
            appendage.loadPrunable(transaction);
            //TODO Why does it need? Take a look how to use it.
//            if (! appendage.verifyVersion()) {
//                throw new AplException.NotValidException("Invalid attachment version " + appendage.getVersion());
//            }
            if (validatingAtFinish) {
                appendage.validateAtFinish(transaction, blockchain.getHeight());
            } else {
                appendage.validate(transaction, blockchain.getHeight());
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

    public boolean verifySignature(Transaction transaction) {
        return checkSignature(transaction) && accountPublicKeyService.setOrVerifyPublicKey(transaction.getSenderId(), transaction.getSenderPublicKey());
    }

    public boolean checkSignature(Transaction transaction) {
        boolean hasValidSignature = transaction.hasValidSignature();
        if (!hasValidSignature) {
            hasValidSignature = transaction.getSignature() != null && Crypto.verify(transaction.getSignature(), transaction.getUnsignedBytes(), transaction.getSenderPublicKey());
            if (hasValidSignature) {
                transaction.withValidSignature();
            }
        }
        return hasValidSignature;
    }

}
