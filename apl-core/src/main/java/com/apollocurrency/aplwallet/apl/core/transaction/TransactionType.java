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
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.Appendix;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.annotation.FeeMarker;
import com.apollocurrency.aplwallet.apl.util.annotation.TransactionFee;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Describes a skeleton for the certain type of transaction's parsing, validation and execution.
 * Every new transaction type should be derived from this class.
 * Template method pattern is used for methods:
 * <ul>
 *     <li>{@link TransactionType#applyUnconfirmed(Transaction, Account)}</li>
 *     <li>{@link TransactionType#apply(Transaction, Account, Account)}</li>
 *     <li>{@link TransactionType#undoUnconfirmed(Transaction, Account)}</li>
 *     <li>{@link TransactionType#undoApply(Transaction, Account, Account)}</li>
 *     <li>{@link TransactionType#validateStateIndependent(Transaction)}  </li>
 *     <li>{@link TransactionType#validateStateDependentAtFinish(Transaction)}</li>
 *     <li>{@link TransactionType#validateStateIndependent(Transaction)} </li>
 * </ul>
 * New tx type developer must never override the methods above, instead of that,
 * implementation of the following methods should be done, respectively to the public template methods in a list above:
 * <ul>
 * <li>{@link TransactionType#applyAttachmentUnconfirmed(Transaction, Account)}</li>
 * <li>{@link TransactionType#applyAttachment(Transaction, Account, Account)}</li>
 * <li>{@link TransactionType#undoAttachmentUnconfirmed(Transaction, Account)} (optional, when {@link TransactionType#applyAttachmentUnconfirmed(Transaction, Account)} do nothing)</li>
 * <li>{@link TransactionType#undoApplyAttachment(Transaction, Account, Account)} (optional, only for transaction, which may fail during execution {@link TransactionType#canFailDuringExecution()} = true) </li>
 * <li>{@link TransactionType#doStateIndependentValidation(Transaction)}</li>
 * <li>{@link TransactionType#doStateDependentValidation(Transaction)}</li>
 * <li>{@link TransactionType#doStateDependentValidationAtFinish(Transaction)} (optional, typically should not be overridden)</li>
 * </ul>
 *
 *
 * <b>Implementation notes:</b>
 * <ol>
 * <li>
 *  {@link TransactionType#applyUnconfirmed(Transaction, Account)} + {@link TransactionType#apply(Transaction, Account, Account)} should be treated as two-step transaction execution,
 *  first is {@link TransactionType#applyUnconfirmed(Transaction, Account)}, which may charge account's unconfirmed balance to freeze asset and/or validate that account has enough assets to transact;
 *  then {@link TransactionType#apply(Transaction, Account, Account)} is executed performing actual transaction execution accounting the effect, done by the {@link TransactionType#applyUnconfirmed(Transaction, Account)}
 * </li>
 * <li>{@link TransactionType#applyAttachment(Transaction, Account, Account)} method should never fail in any case, excluding when it is defined by {@link TransactionType#canFailDuringExecution()} = true)</li>
 * <li>{@link TransactionType#undoAttachmentUnconfirmed(Transaction, Account)} should fully cancel {@link TransactionType#applyAttachmentUnconfirmed(Transaction, Account)}</li>
 * <li>{@link TransactionType#undoApplyAttachment(Transaction, Account, Account)} should cancel all successfully done actions by {@link TransactionType#applyAttachment(Transaction, Account, Account)}</li>
 * <li>{@link TransactionType#doStateIndependentValidation(Transaction)}  + {@link TransactionType#doStateDependentValidation(Transaction)} should be treated as two-layered transaction validation, which forms full transaction validation flow,
 *  after successfully passing these validations transaction will be considered as fully valid against the current blockchain rules and state</li>
 * <li>{@link TransactionType#doStateDependentValidationAtFinish(Transaction)} should pass when {@link TransactionType#applyAttachmentUnconfirmed(Transaction, Account)} is executed (required for phasing transactions successful execution), whereas
 *  {@link TransactionType#doStateDependentValidation(Transaction)} may fail on already added into a blockchain phased transaction</li>
 * </ol>
 *
 */
@Getter
@Slf4j
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
        Map<String, Integer> typeDuplicates = duplicates.computeIfAbsent(uniqueType, k -> new HashMap<>());
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

    /**
     * Marker method, indicating whether transaction can fail during execution of the {@link TransactionType#applyAttachment(Transaction, Account, Account)}
     * <p>Should be overridden by new tx type, which can fail</p>
     * @return false if transaction must not fail during execution (default for most existing tx types), otherwise - true
     */
    public boolean canFailDuringExecution() {
        return false;
    }

    /**
     * @return new transaction type spec, which should be defined under enum {@link TransactionTypes.TransactionTypeSpec}
     */
    public abstract TransactionTypes.TransactionTypeSpec getSpec();

    /**
     * @return ledger event, perfomed by transction type, which should be defined under enum {@link LedgerEvent}
     */
    public abstract LedgerEvent getLedgerEvent();

    /**
     * Parse attachment for the transaction type from serialized bytes using a byte buffer
     * @param buffer byte buffer, where attachment's serialized bytes are located
     * @return deserialized attachment for this transaction type
     * @throws AplException.NotValidException when any validation error occurred during deserialization (avoid throwing it)
     */
    public abstract AbstractAttachment parseAttachment(ByteBuffer buffer) throws AplException.NotValidException;

    /**
     * Parse attachment for the transaction type from a serialized json object
     * @param attachmentData json object, containing attachment json serialized data
     * @return deserialized attachmen for thi transaction type
     * @throws AplException.NotValidException when any validation error occurred during deserialization (avoid throwing it)
     */
    public abstract AbstractAttachment parseAttachment(JSONObject attachmentData) throws AplException.NotValidException;


    /**
     * Performs transaction's validation (transaction itself and its attachment) using current blockchain state only, no state-independent validation will be done
     * <p>Should be used in conjuction with {@link TransactionType#validateStateIndependent(Transaction)}, which will guarantee full tx structure and attachment validation</p>
     * <p>Validation should pass for valid transaction only on transaction's acceptance height, when it is included in a block, which will be pushed into a blockchain.
     * Successful validation for the phased transactions on any height, except of acceptance height, is not guaranteed,
     * use {@link TransactionType#validateStateDependentAtFinish(Transaction)} instead</p>
     * @param transaction transaction of this type to validate using blockchain state
     * @throws AplException.ValidationException when transaction doesn't pass validation for any reason
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException same as above
     * but preferred and when sender's account has enough money to cover transaction fee, but not a transferring amount
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException in cases,
     * when transaction is correct but sender's account is not exist or has not enough funds to pay fee
     */
    public final void validateStateDependent(Transaction transaction) throws AplException.ValidationException {
        TransactionAmounts amounts = new TransactionAmounts(transaction);
        Account account = accountService.getAccount(transaction.getSenderId());
        if (account == null) {
            throw new AplUnacceptableTransactionValidationException(
                String.format("Transaction's sender not found, tx: %s, sender: %s", transaction.getStringId(),
                    Long.toUnsignedString(transaction.getSenderId())), transaction);
        }
        if (account.getUnconfirmedBalanceATM() < amounts.getTotalAmountATM()) {
            if (account.getUnconfirmedBalanceATM() < amounts.getFeeATM()) {
                throw new AplUnacceptableTransactionValidationException(
                    String.format("Not enough apl balance on account: %s, required at least %d, but only got %d",
                        Long.toUnsignedString(transaction.getSenderId()), amounts.getTotalAmountATM(), account.getUnconfirmedBalanceATM()), transaction);
            } else {
                throw new AplAcceptableTransactionValidationException(String.format("Not enough apl balance on account: %s"
                    + " to pay transaction both amount: %d and fee: %d, only fee may be paid, balance: %s",
                    Long.toUnsignedString(transaction.getSenderId()),amounts.getAmountATM(), amounts.getFeeATM(), account.getUnconfirmedBalanceATM()), transaction);
            }
        }
        doStateDependentValidation(transaction);
    }

    /**
     * Do almost the same validation as {@link TransactionType#validateStateDependent(Transaction)} but on the height of the phasing
     * transaction execution, not addition into a blockchain
     * <p>This method will account all the changes done by the {@link TransactionType#applyUnconfirmed(Transaction, Account)}</p>
     * @param transaction phasing transaction to validate at the height of execution
     * @throws AplException.ValidationException when transaction's doesn't pass validation for any reason
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException same as above, but preferred
     */
    public final void validateStateDependentAtFinish(Transaction transaction) throws AplException.ValidationException {
        doStateDependentValidationAtFinish(transaction);
    }

    /**
     * Validate transaction of this type without using current state data. Ensures that transaction has a correct
     * form and may be (but not must be) included in a blockchain with no state changes (as failed)
     * @param transaction transaction of this type to validate without state data
     * @throws AplException.ValidationException when transaction and its data has incorrect form and is inappropriate for a blockchain
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException same as above, but preferred
     */
    public final void validateStateIndependent(Transaction transaction) throws AplException.ValidationException {
        doStateIndependentValidation(transaction);
    }

    /**
     * Allows transaction's of same/different type, that modify the same resource (account's
     * balance, currency/asset balance, etc) to be included in a one block without conflicts (e.g double spending)
     * <p>This capability consist of checking that account has enough funds to pay transaction amount and fee and freezing tx's entire amount to guarantee
     * no double spending from other transactions, pretending on the same sender's funds. Transaction types implement
     * the same freeze/unconfirmed state persistence for its specific attachments (if necessary) {@link TransactionType#applyAttachmentUnconfirmed(Transaction, Account)}  </p>
     * <p>Represent a first step of a transaction execution</p>
     * <p>Restore modified data, if double spending occurred</p>
     * @return false if double spending, otherwise true
     */
    @TransactionFee(FeeMarker.UNCONFIRMED_BALANCE)
    public final boolean applyUnconfirmed(Transaction transaction, Account senderAccount) {
        TransactionAmounts amounts = new TransactionAmounts(transaction);
        if (senderAccount.getUnconfirmedBalanceATM() < amounts.getTotalAmountATM()) {
            return false;
        }
        accountService.addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), -amounts.amountATM, -amounts.feeATM);
        if (!applyAttachmentUnconfirmed(transaction, senderAccount)) {
            accountService.addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(), amounts.amountATM, amounts.feeATM);
            return false;
        }
        return true;
    }

    /**
     * Perform transaction's first step execution (contention resource validation and change persistence)
     * for the specific attachment of the TransactionType
     * <p>Should restore modified data, when return false</p>
     * <p>Should be compensated by the {@link TransactionType#undoApplyAttachment(Transaction, Account, Account)}</p>
     * @param transaction transaction, which attachment should be processed
     * @param senderAccount transaction's sender account
     * @return false, when conflict or double spending occur, otherwise true
     */
    protected abstract boolean applyAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    /**
     * Execute transaction transferring transaction amount from sender's account to the recipient if any and perform tx type
     * specific action by the method {@link TransactionType#applyAttachment(Transaction, Account, Account)}
     * <p>Must not be overridden</p>
     * @param transaction transaction to execute
     * @param senderAccount transaction's sender
     * @param recipientAccount transaction's recipient (may be null)
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException when transaction
     * supports failing during execution {@link TransactionType#canFailDuringExecution() = true}
     */
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
                recipientAccount.setPublicKey(senderAccount.getPublicKey());
            }
            accountService.addToBalanceAndUnconfirmedBalanceATM(recipientAccount, getLedgerEvent(), transactionId, amount);
            log.info("{} transferred {} ATM to the recipient {}", senderAccount.balanceString(), transaction.getAmountATM(), recipientAccount.balanceString());
        }
        applyAttachment(transaction, senderAccount, recipientAccount);
    }

    /**
     * Compensate all the changes done by the transaction execution from the method {@link TransactionType#apply(Transaction, Account, Account)}
     * <p>Typically should be invoked for reverting transaction, which failed during execution</p>
     * <p>Transaction type specific 'undo' procedures should be implemented by the {@link TransactionType#undoApplyAttachment(Transaction, Account, Account)}</p>
     * <p>For transaction types, which support failing during execution, in-memory action log is recommended to don't rollback partially executed actions</p>
     * <p>Must not be overridden</p>
     * @param transaction transaction to undo
     * @param senderAccount transaction's sender
     * @param recipientAccount transaction's recipient
     */
    public final void undoApply(Transaction transaction, Account senderAccount, Account recipientAccount) {
        undoApplyAttachment(transaction, senderAccount, recipientAccount);
        long amount = transaction.getAmountATM();
        long transactionId = transaction.getId();
        if (!transaction.attachmentIsPhased()) {
            accountService.addToBalanceATM(senderAccount, getLedgerEvent(), transactionId, amount, transaction.getFeeATM());
        } else {
            accountService.addToBalanceATM(senderAccount, getLedgerEvent(), transactionId, amount);
        }
        if (recipientAccount != null) {
            //refresh balance in case a debit account is equal to a credit one
            if (Objects.equals(senderAccount.getId(), recipientAccount.getId())) {
                recipientAccount.setBalanceATM(senderAccount.getBalanceATM());
            }
            accountService.addToBalanceAndUnconfirmedBalanceATM(recipientAccount, getLedgerEvent(), transactionId, -amount);
            log.info("{} was refunded {} ATM by the recipient {}", senderAccount.balanceString(), transaction.getAmountATM(), recipientAccount.balanceString());
        }
    }

    /**
     * Execute transaction type specific actions depending on transaction attachment and other data
     * <p>Not intended to be called directly, since this method will be called from {@link TransactionType#apply(Transaction, Account, Account)}</p>
     *
     * @param transaction transaction, which should be executed specifically for the transaction type
     * @param senderAccount transaction's sender
     * @param recipientAccount transaction's recipient (may be null)
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplTransactionExecutionException when transaction type
     * supports failing during execution
     */
    public abstract void applyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount);

    /**
     * Compensate all the changes done by the {@link TransactionType#applyUnconfirmed(Transaction, Account)}
     * <p>Transaction type specific compensation actions should be performed by implementation of the {@link TransactionType#undoAttachmentUnconfirmed(Transaction, Account)} method</p>
     * @param transaction transaction to undo freeze/validation changes
     * @param senderAccount transaction's sender
     */
    @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE)
    public final void undoUnconfirmed(Transaction transaction, Account senderAccount) {
        undoAttachmentUnconfirmed(transaction, senderAccount);
        TransactionAmounts amounts = new TransactionAmounts(transaction);
        accountService.addToUnconfirmedBalanceATM(senderAccount, getLedgerEvent(), transaction.getId(),
            amounts.getAmountATM(), amounts.getFeeATM());
    }

    /**
     * Compensate transaction type specific freeze/validation changes done by the {@link TransactionType#applyAttachmentUnconfirmed(Transaction, Account)}.
     * <p>Typically should not be called directly, {@link TransactionType#undoUnconfirmed(Transaction, Account)} should be used instead</p>
     * @param transaction transaction, which freeze/validation/unconfirmed changes should be reverted for specific tx type
     * @param senderAccount transaction's sender
     */
    @TransactionFee(FeeMarker.UNDO_UNCONFIRMED_BALANCE)
    public abstract void undoAttachmentUnconfirmed(Transaction transaction, Account senderAccount);

    /**
     * Check whether transaction is a duplicate under duplicates map, which should be updated (in most cases)
     * during check using principle defined by the {@link TransactionType#isDuplicate(TransactionTypes.TransactionTypeSpec, String, Map, int)}
     * <p>Will not be called for phasing transaction on transaction's acceptance height, but will be called on transaction's execution height</p>
     * @param transaction transaction, that should be checked for duplicate
     * @param duplicates global duplicates map, which is shared among the all transactions which should be checked for duplicates
     * @return true, when transaction is a duplicate, otherwise false
     */
    public boolean isDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return false;
    }

    /**
     * Typically do the same as {@link TransactionType#isDuplicate}  and share the same duplicates map, but isBlockDuplicate check
     * is done first on transaction's acceptance height (when transaction included into a blockchain).
     * <p>Will be called for phasing and standard transaction on transaction's acceptance height only</p>
     *  @param transaction transaction, that should be checked for duplicates
     *  @param duplicates global duplicates map, which is shared among the all transactions which should be checked for duplicates
     *  @return true, when transaction is a duplicate, otherwise false
     */
    public boolean isBlockDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return false;
    }

    /**
     * Verify duplicates on mempool level, currently is not used and should not be implemented
     * @param transaction transaction to check
     * @param duplicates global duplicates map
     * @return false only
     */
    @Deprecated
    public boolean isUnconfirmedDuplicate(Transaction transaction, Map<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates) {
        return false;
    }

    /**
     * Check, whether transaction is pruned (data was expired and cleared) or not
     * @param transactionId id of the transaction to check
     * @return false for not pruned transaction, otherwise true
     */
    public boolean isPruned(long transactionId) {
        return false;
    }

    /**
     * Specify, whether transaction can have recipient or not
     * @return true, when recipient is allowed, otherwise - false
     */
    public abstract boolean canHaveRecipient();

    /**
     * Almost the same as {@link TransactionType#canHaveRecipient()} but more strict, primarily should be used for validation
     * @return true, when transaction should be with a recipient field, otherwise - false
     */
    public boolean mustHaveRecipient() {
        return canHaveRecipient();
    }

    /**
     * Is not used in blockchain logic. Required for info purposes only
     */
    @Deprecated
    public abstract boolean isPhasingSafe();

    /**
     * Specify, that transaction of the specific type may be phased (delayed until some event occurred)
     * @return true, when phasing is allowed, otherwise false
     */
    public boolean isPhasable() {
        return true;
    }

    /**
     * Specify current fee for the transaction of a specific type
     * <p>Fee may be created using {@link TransactionType.FeeFactory} class, allowing flexible fee configs via chains.json
     * and {@link com.apollocurrency.aplwallet.apl.util.env.config.TransactionFeeSettings}</p>
     * <p>Default fee is whole one unit</p>
     * @param transaction transaction, to get fee for
     * @return fee object, which allows fee calculation for transactions of this type
     */
    @TransactionFee(FeeMarker.BASE_FEE)
    public Fee getBaselineFee(Transaction transaction) {
        return getFeeFactory().createFixed(BigDecimal.ONE);
    }

    /**
     * Fee updating mechanism, which was replaced by fee configs, defined under chains.json. Will be removed in future
     */
    @TransactionFee(FeeMarker.FEE)
    @Deprecated(forRemoval = true)
    public Fee getNextFee(Transaction transaction) {
        return getBaselineFee(transaction);
    }

    /**
     * Fee updating mechanism, which was replaced by fee configs, defined under chains.json. Will be removed in future
     */
    @Deprecated(forRemoval = true)
    public int getBaselineFeeHeight() {
        return 1;
    }

    /**
     * Fee updating mechanism, which was replaced by fee configs, defined under chains.json. Will be removed in future
     */
    @Deprecated(forRemoval = true)
    public int getNextFeeHeight() {
        return Integer.MAX_VALUE;
    }

    /**
     * Return array of splitted transaction fee into 3 parts to award to the last 3 forgers to enable more uniform fee
     * distribution.
     * <p>Should be avoided from usage in new transaction types, because it leads to the complicated and ambiguous logic</p>
     * @param transaction transaction, which fee should be splitted
     * @return array of 3 length, with transaction fee splitted
     */
    @Deprecated
    @TransactionFee(FeeMarker.FEE)
    public long[] getBackFees(Transaction transaction) {
        return Convert.EMPTY_LONG;
    }

    /**
     * Specify unique transaction type's name
     * @return unique tx type's name
     */
    public abstract String getName();

    /**
     * @return string representation of the transaction type including its spec and name
     */
    @Override
    public final String toString() {
        return getName() + " " + getSpec();
    }

    /**
     * Handy class to create transaction fee objects along with the custom transaction fee configs, loaded from the chains.json.
     * Fee factory offers a defaults support also
     */
    public class FeeFactory {
        /**
         * Create fee object for size-based fee calculation using specific size function and unit size to count
         * <p>Supports chains.json fee configuration, when specified, if not given default values will be used</p>
         * @param defaultConstantAPL default constant fee value in APL to use for fee calculation, when fee config from chain.json is absent
         * @param defaultSizedBasedFeeAPL default size-based fee value in APL to use for fee calculation, when fee config from chains.json is absent
         * @param sizeFunction function, which calculates size on transaction and its appendage
         * @param unitSize size of the transaction data, which should be treated as one unit, every unit will be accounted by the size-based fee value
         * @return size-based fee object to calculate fee for the transaction
         */
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

        /**
         * Same as {@link TransactionType.FeeFactory#createSizeBased(BigDecimal, BigDecimal, BiFunction, int)} but unitSize of
         * 32 is supplied (default for most transaction types)
         */
        public Fee createSizeBased(BigDecimal defaultConstantAPL, BigDecimal defaultSizedBasedFeeAPL, BiFunction<Transaction, Appendix, Integer> sizeFunction) {
            return createSizeBased(defaultConstantAPL, defaultSizedBasedFeeAPL, sizeFunction, 32);
        }

        /**
         * Create fixed fee object using configured fee for tx of a certain type in chains.json or using default supplied value in case of absent fee config
         * @param defaultConstantAPL default fee value in APL to use for fee calculation, when fee config from chain.json is absent
         * @return fixed fee object for the transaction type
         */
        public Fee createFixed(BigDecimal defaultConstantAPL) {
            long oneAPL = blockchainConfig.getOneAPL();
            long constantFeeATM = blockchainConfig.getCurrentConfig().getBaseFee(getSpec(), defaultConstantAPL).multiply(BigDecimal.valueOf(oneAPL)).longValueExact();
            return new Fee.ConstantFee(constantFeeATM);
        }

        /**
         * Create custom fee object, which will calculate fee by the supplied function
         * @param feeCalc custom fee calculation function
         * @return fee object for a custom fee calculation
         */
        public Fee createCustom(BiFunction<Transaction, Appendix, Long> feeCalc) {
            return (feeCalc::apply);
        }

    }

    /**
     * Perform transaction type's specific validation against a state, when phased transaction should be executed.
     * <p>Typically should not be called directly, use {@link TransactionType#validateStateDependentAtFinish(Transaction)}</p>
     * <p>In most cases should not be overridden</p>
     * @param transaction phased transaction to validate at execution's height
     * @throws AplException.ValidationException when transaction is not valid (avoid throwing it)
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplTransactionValidationException when transaction is not valid for some meaningful reason (preferred way)
     */
    protected void doStateDependentValidationAtFinish(Transaction transaction) throws AplException.ValidationException {
        doStateDependentValidation(transaction);
    }

    /**
     * Perform transaction type's specific form and structure validation of the given transaction
     * <p>Transaction, that doesn't pass this validation will be considered as totally invalid and should not be included into a blockchain</p>
     * <p>This method must not be used directly, use {@link TransactionType#validateStateIndependent(Transaction)} instead</p>
     * @param transaction transaction to validate
     * @throws AplException.ValidationException when transaction doesn't pass validation (throwing it should be avoided)
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException when transaction doesn't pass validation for some meaningful reason (preferred way)
     */
    protected abstract void doStateIndependentValidation(Transaction transaction) throws AplException.ValidationException;


    /**
     * Perform transaction type's specific validation against the current blockchain state, typically fetching all the stored/cached blockchain data
     * <p>Transaction that doesn't pass this validation will be considered as acceptable to be added into a blockchain, but should be marked as failed and not executed </p>
     * @param transaction transaction to validate
     * @throws AplException.ValidationException when transaction doesn't pass validation (throwing it should be avoided)
     * @throws com.apollocurrency.aplwallet.apl.core.exception.AplAcceptableTransactionValidationException when transaction doesn't pass validation against current state (preferred way)
     */
    protected abstract void doStateDependentValidation(Transaction transaction) throws AplException.ValidationException;

    /**
     * Compensate all the changes done by the {@link TransactionType#applyAttachment(Transaction, Account, Account)}
     * <p>By default such type of compensation is not allowed, to enable it, a new transaction type should override this
     * method along with specifying {@link TransactionType#canFailDuringExecution()} = true</p>
     * <p>This method must not be used directly, use {@link TransactionType#undoApply(Transaction, Account, Account)} instead</p>
     * @param transaction transaction to revert
     * @param senderAccount transaction's sender
     * @param recipientAccount transaction' recipient (may be null)
     */
    protected void undoApplyAttachment(Transaction transaction, Account senderAccount, Account recipientAccount) {
        if (!canFailDuringExecution()) {
            throw new UnsupportedOperationException("undoApplyAttachment is not supported for transaction type: " + getSpec() + ", transaction: " + transaction.getStringId() + ", sender: " + Long.toUnsignedString(senderAccount.getId()));
        }
    }

    @Getter
    private class TransactionAmounts {
        private final long feeATM;
        private final long amountATM;

        public TransactionAmounts(Transaction transaction) {
            long amountATM = transaction.getAmountATM();
            long feeATM = transaction.getFeeATM();
            if (transaction.referencedTransactionFullHash() != null) {
                feeATM = Math.addExact(feeATM, blockchainConfig.getUnconfirmedPoolDepositAtm());
            }
            this.feeATM = feeATM;
            this.amountATM = amountATM;
        }

        public long getTotalAmountATM() {
            return Math.addExact(amountATM, feeATM);
        }
    }
}
