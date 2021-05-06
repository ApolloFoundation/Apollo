/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.Getter;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

@Getter
public abstract class TransactionType {
    private final BlockchainConfig blockchainConfig;
    private final AccountService accountService;
    private final FeeFactory feeFactory;

    public TransactionType(BlockchainConfig blockchainConfig, AccountService accountService) {
        this.blockchainConfig = blockchainConfig;
        this.accountService = accountService;
        this.feeFactory = new FeeFactory();
    }

    public static boolean isDuplicate(TransactionTypes.TransactionTypeSpec uniqueType, String key, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, boolean exclusive) {
        return isDuplicate(uniqueType, key, duplicates, exclusive ? 0 : Integer.MAX_VALUE);
    }

    public static boolean isDuplicate(TransactionTypes.TransactionTypeSpec uniqueType, String key, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates, int maxCount) {
        Map<String, Integer> typeDuplicates = duplicates.get(uniqueType);
        if (typeDuplicates == null) {
            typeDuplicates = new HashMap<>();
            duplicates.put(uniqueType, typeDuplicates);
        }
        Integer currentCount = typeDuplicates.get(key);
        if (currentCount == null) {
            typeDuplicates.put(key, maxCount > 0 ? 1 : 0);
            return false;
        }
        if (currentCount == 0) {
            return true;
        }
        if (currentCount < maxCount) {
            typeDuplicates.put(key, currentCount + 1);
            return false;
        }
        return true;
    }

    public abstract TransactionTypes.TransactionTypeSpec getSpec();

    public abstract LedgerEvent getLedgerEvent();

    public abstract AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException;

    public abstract AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException;

    public abstract void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException;

    public abstract void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException;

    // return false if double spending
    @TransactionFee(FeeMarker.UNCONFIRMED_BALANCE)
    public final boolean applyUnconfirmed(Transaction transaction, Account senderAccount) {
        long amountATM = transaction.getAmountATM();
        long feeATM = transaction.getFeeATM();
        if (transaction.referencedTransactionFullHash() != null) {
            feeATM = Math.addExact(feeATM, blockchainConfig.getUnconfirmedPoolDepositAtm());
        }
        long totalAmountATM = Math.addExact(amountATM, feeATM);
        if (senderAccount.getUnconfirmedBalanceATM() < totalAmountATM) {
            return false;
        }
        accountService.addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -amountATM, -feeATM);
        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            accountService.addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), amountATM, feeATM);
            return false;
        }
        return true;
    }

    public abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public void apply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        long amount = transaction.getAmountATM();
        long transactionId = transaction.getId();
        if (!transaction.attachmentIsPhased()) {
            accountService.addToBalanceATM(senderAccount, getLedgerEvent(), transactionId, -amount, -transaction.getFeeATM());
        } else {
            accountService.addToBalanceATM(senderAccount, getLedgerEvent(), transactionId, -amount);
        }
        if (recipientAccount != null) {
            //refresh balance in case a debit account is equal to a credit one
            if (Objects.equals(senderAccount.getId(), recipientAccount.getId())) {
                recipientAccount.setBalanceATM(senderAccount.getBalanceATM());
            }
            accountService.addToBalanceAndUnconfirmedBalanceATM(recipientAccount, getLedgerEvent(), transactionId, amount);
        }
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    public abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE)
    public final void undoUnconfirmed(Transaction transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        accountService.addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(),
            transaction.getAmountATM(), transaction.getFeeATM());
        if (transaction.referencedTransactionFullHash() != null) {
            accountService.addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), 0,
                blockchainConfig.getUnconfirmedPoolDepositAtm());
        }
    }

    @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE)
    public abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return false;
    }

    // isBlockDuplicate and isDuplicate share the same duplicates map, but isBlockDuplicate check is done first
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return false;
    }

    public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return false;
    }

    public boolean isPruned(long transactionId) {
        return false;
    }

    public abstract boolean canHaveRecipient();

    public boolean mustHaveRecipient() {
        return canHaveRecipient();
    }

    /**
     * Is not used in blockchain logic. Required for info purposes only
     */
    @Deprecated
    public abstract boolean isPhasingSafe();

    public boolean isPhasable() {
        return true;
    }

    @TransactionFee(FeeMarker.BASE_FEE)
    public Fee getBaselineFee(Transaction transaction) {
        return getFeeFactory().createFixed(BigDecimal.ONE);
    }

    @TransactionFee(FeeMarker.FEE)
    public Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    public int getBaselineFeeHeight() {
        return 1;
    }

    public int getNextFeeHeight() {
        return Integer.MAX_VALUE;
    }

    @TransactionFee(FeeMarker.FEE)
    public long[] getBackFees(Transaction transaction) {
        return Convert.EMPTY_LONG;
    }

    public abstract String getName();

    @Override
    public final String toString() {
        return getName() + " " + getSpec();
    }

    public class FeeFactory {
        public Fee createSizeBased(BigDecimal defaultConstantAPL, BigDecimal defaultSizedBasedFeeAPL, BiFunction<Transaction, Appendix, Integer> sizeFunction, int unitSize) {
            BlockchainConfig blockchainConfig = getBlockchainConfig();
            HeightConfig heightConfig = blockchainConfig.getCurrentConfig();
            long oneAPL = blockchainConfig.getOneAPL();
            long constantFeeATM = heightConfig.getBaseFee(getSpec(), defaultConstantAPL).multiply(BigDecimal.valueOf(oneAPL)).longValueExact();
            long feePerSizeATM = heightConfig.getSizeBasedFee(getSpec(), defaultSizedBasedFeeAPL).multiply(BigDecimal.valueOf(oneAPL)).longValueExact();
            return new Fee.SizeBasedFee(constantFeeATM, feePerSizeATM, unitSize) {
                @Override
                public int getSize(Transaction transaction, Appendix appendage) {
                    return sizeFunction.apply(transaction, appendage);
                }
            };
        }

        public Fee createSizeBased(BigDecimal defaultConstantAPL, BigDecimal defaultSizedBasedFeeAPL, BiFunction<Transaction, Appendix, Integer> sizeFunction) {
            return createSizeBased(defaultConstantAPL, defaultSizedBasedFeeAPL, sizeFunction, 32);
        }

        public Fee createFixed(BigDecimal defaultConstantAPL) {
            long oneAPL = blockchainConfig.getOneAPL();
            long constantFeeATM = blockchainConfig.getCurrentConfig().getBaseFee(getSpec(), defaultConstantAPL).multiply(BigDecimal.valueOf(oneAPL)).longValueExact();
            return new Fee.ConstantFee(constantFeeATM);
        }

        public Fee createCustom(BiFunction<Transaction, Appendix, Long> feeCalc) {
            return (feeCalc::apply);
        }

    }
}
