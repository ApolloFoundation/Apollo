/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.exception.AplUnacceptableTransactionValidationException;
import com.apollocurrency.aplwallet.apl.core.model.CreateTransactionRequest;
import com.apollocurrency.aplwallet.apl.core.model.EcBlockData;
import com.apollocurrency.aplwallet.apl.core.model.PhasingParams;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.rest.TransactionCreator;
import com.apollocurrency.aplwallet.apl.core.service.appdata.TimeService;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionBuilderFactory;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionProcessor;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSigner;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.TransactionSignerImpl;
import com.apollocurrency.aplwallet.apl.core.service.state.PhasingPollService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountControlPhasingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountPublicKeyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.signature.Signature;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AbstractAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.EncryptedToSelfMessageAppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.OrdinaryPaymentAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableEncryptedMessageAppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.types.payment.OrdinaryPaymentTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionValidatorTest {
    public static final long SENDER_ID = 1L;
    BlockchainConfig blockchainConfig;
    @Mock
    PhasingPollService phasingPollService;
    @Mock
    Blockchain blockchain;
    @Mock
    FeeCalculator feeCalculator;
    @Mock
    AccountPublicKeyService accountPublicKeyService;
    @Mock
    AccountControlPhasingService accountControlPhasingService;
    @Mock
    PrunableLoadingService prunableService;
    @Mock
    TimeService timeService;
    @Mock
    TransactionVersionValidator transactionVersionValidator;
    @Mock
    AppendixValidatorRegistry validatorRegistry;
    AccountService accountService;

    @Mock
    Chain chain;
    @Mock
    HeightConfig heightConfig;
    @Mock
    Transaction tx;
    @Mock
    TransactionType type;
    @Mock
    AbstractAttachment attachment;
    @Mock
    Account sender;

    TransactionTestData td;

    TransactionValidator validator;

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();
        accountService = td.getAccountService();
        blockchainConfig = td.getBlockchainConfig();
        doReturn(chain).when(blockchainConfig).getChain();
        validator = new TransactionValidator(blockchainConfig, phasingPollService, blockchain, feeCalculator, accountService, accountPublicKeyService, accountControlPhasingService, transactionVersionValidator, prunableService, validatorRegistry);
    }

    @Test
    void getFinishValidationHeightNoPhasing() {
        doReturn(10).when(blockchain).getHeight();

        int height = validator.getFinishValidationHeight(td.TRANSACTION_4, td.TRANSACTION_4.getAttachment());

        assertEquals(10, height);
    }

    @Test
    void getFinishValidationHeightWithPhasing() {
        int height = validator.getFinishValidationHeight(td.TRANSACTION_13, td.TRANSACTION_13.getAttachment());

        assertEquals(536999, height);
    }

    @Test
    void validateLightlyOK() {
        doReturn(true).when(transactionVersionValidator).isValidVersion(td.TRANSACTION_0);
        HeightConfig heightConfig = mockMaxBalance();
        doReturn(8).when(blockchainConfig).getDecimals();
        doReturn(30_000_000L).when(blockchainConfig).getInitialSupply();
        doReturn(100_000_000L).when(blockchainConfig).getOneAPL();

        validator.validateLightly(td.TRANSACTION_0);
    }

    @Test
    void validateSufficiently() {
        Transaction transaction = createTransaction();
        doReturn(true).when(transactionVersionValidator).isValidVersion(transaction);
        HeightConfig heightConfig = mockMaxBalance();
        Account senderAcc = new Account(transaction.getSenderId(), transaction.getAmountATM() + transaction.getFeeATM(), transaction.getAmountATM() + transaction.getFeeATM(), 0, 0, 0);
        doReturn(senderAcc).when(accountService).getAccount(transaction.getSenderId());
        doReturn(true).when(accountPublicKeyService).setOrVerifyPublicKey(transaction.getSenderId(), transaction.getSenderPublicKey());
        doReturn(100).when(heightConfig).getMaxArbitraryMessageLength();
        doReturn(2000).when(blockchain).getHeight();
        doAnswer(invocation -> {
            if (invocation.getArgument(0).equals(transaction.getMessage())) {
                return new MessageAppendixValidator(blockchainConfig);
            } else if (invocation.getArgument(0).equals(transaction.getPhasing())) {
                return new PhasingAppendixValidator(blockchain, phasingPollService, blockchainConfig);
            } else {
                return null;
            }
        }).when(validatorRegistry).getValidatorFor(any());

        validator.validateSufficiently(transaction);
    }

    @Test
    void validateSufficiently_invalidTxVersion() {
        Transaction transaction = createTransaction();
        doReturn(false).when(transactionVersionValidator).isValidVersion(transaction);

        validateSufficientlyWithEx(transaction, "Unsupported transaction version 1 at height 0");
    }


    @Test
    void validateSufficiently_typeSpecIsNull() {
        mockMaxBalance();
        doReturn(type).when(tx).getType();
        doReturn(true).when(transactionVersionValidator).isValidVersion(tx);

        validateSufficientlyWithEx(tx, createBadTxFieldsExMessage(0, 0,0,0));
    }

    @Test
    void validateSufficiently_timestampIsZero_deadlineIsNotZero() {
        executeBadTxFieldsScenario(0, 0, 1, 0);
    }

    @Test
    void validateSufficiently_timestampIsZero_feeATMIsNotZero() {
        executeBadTxFieldsScenario(0, 100, 0, 0);
    }

    @Test
    void validateSufficiently_timestampIsNotZero_deadlineLessThanZero() {
        executeBadTxFieldsScenario(900, 0, -2, 0);
    }

    @Test
    void validateSufficiently_timestampIsNotZero_feeLessThanZero() {
        executeBadTxFieldsScenario(900, -10, 1440, 0);
    }

    @Test
    void validateSufficiently_zeroTx_feeAboveMaxBalance() {
        executeBadTxFieldsScenario(0, Long.MAX_VALUE, 0, 0);
    }

    @Test
    void validateSufficiently_amountLessThanZero() {
        executeBadTxFieldsScenario(1000, 125, 1440, -1000);
    }

    @Test
    void validateSufficiently_amountAboveMaxBalance() {
        executeBadTxFieldsScenario(1000, 125, 1440, Long.MAX_VALUE);
    }

    @Test
    void validateSufficiently_badReferencedHash() {
        prepareScenarioWithTxFields(1000, 10, 1440, 250);
        doReturn("ffff0000").when(tx).getReferencedTransactionFullHash();

        validateSufficientlyWithEx(tx, "Invalid referenced transaction full hash ffff0000");
    }

    @Test
    void validateSufficiently_OkReferencedHash_noAttachment() {
        prepareScenarioWithTxFields(1000, 10, 1440, 250);
        doReturn("0000000000000000000000000000000000000000000000000000000000000000").when(tx).getReferencedTransactionFullHash();

        validateSufficientlyWithEx(tx, "Invalid attachment null for transaction of type type");
    }

    @Test
    void validateSufficiently_attachmentOfDifferentType() {
        prepareScenarioWithTxFields(1000, 10, 1440, 250);
        doReturn(attachment).when(tx).getAttachment();
        doReturn(TransactionTypes.TransactionTypeSpec.ACCOUNT_PROPERTY).when(attachment).getTransactionTypeSpec();

        validateSufficientlyWithEx(tx, "Invalid attachment attachment for transaction of type type");
    }

    @Test
    void validateSufficiently_amountIsNotZeroForTxTypeWithRecipientNotAllowed() {
        prepareScenarioWithTxFields(1000, 10, 1440, 250);
        mockAttachment();

        validateSufficientlyWithEx(tx, "Transactions of this type must have recipient == 0, amount == 0");
    }

    @Test
    void validateSufficiently_recipientIsNotZeroForTxTypeWithRecipientNotAllowed() {
        prepareScenarioWithTxFields(1000, 10, 1440, 0);
        mockAttachment();
        doReturn(1L).when(tx).getRecipientId();

        validateSufficientlyWithEx(tx, "Transactions of this type must have recipient == 0, amount == 0");
    }

    @Test
    void validateSufficiently_transactionTypeNotAllowEmptyRecipient() {
        prepareScenarioWithTxFields(1000, 10, 1440, 0);
        doReturn(true).when(type).canHaveRecipient();
        doReturn(true).when(type).mustHaveRecipient();
        mockAttachment();

        validateSufficientlyWithEx(tx, "Transactions of this type must have a valid recipient");
    }

    @Test
    void validateSufficiently_transactionWithCorrectRecipient_wrongAppendixVersion() {
        prepareScenarioWithTxFields(1000, 10, 1440, 0);
        doReturn(true).when(type).canHaveRecipient();
        doReturn(true).when(type).mustHaveRecipient();
        doReturn(1L).when(tx).getRecipientId();
        doReturn(false).when(attachment).verifyVersion();
        doReturn("Test attachment").when(attachment).getAppendixName();
        doReturn(List.of(attachment)).when(tx).getAppendages();
        mockAttachment();

        validateSufficientlyWithEx(tx, "Test attachment appendage version '0' is not supported");
    }

    @Test
    void validateSufficiently_transactionWithoutRecipient_attachmentValidationFailed() throws AplException.ValidationException {
        prepareScenarioWithTxFields(1000, 10, 1440, 0);
        doReturn(true).when(type).canHaveRecipient();
        doReturn(false).when(type).mustHaveRecipient();
        doReturn(true).when(attachment).verifyVersion();
        doReturn(List.of(attachment)).when(tx).getAppendages();
        mockAttachment();
        doThrow(new AplException.NotValidException("Test attachment ex")).when(attachment).performStateIndependentValidation(tx, 0);

        validateSufficientlyWithEx(tx, "Test attachment ex");
    }

    @Test
    void validateSufficiently_noSenderAccount() throws AplException.ValidationException {
        prepareLightValidationSuccessfulScenario();
        doReturn(1L).when(tx).getSenderId();

        validateSufficientlyWithEx(tx, "Account 1 does not exist yet");

        verify(attachment).performStateIndependentValidation(tx, 0);
    }

    @Test
    void validateSufficiently_feeIsTooSmall() throws AplException.ValidationException {
        prepareLightValidationSuccessfulScenario();
        doReturn(SENDER_ID).when(tx).getSenderId();
        doReturn(sender).when(accountService).getAccount(SENDER_ID);
        doReturn(20L).when(feeCalculator).getMinimumFeeATM(tx, 0);
        doReturn(10L).when(blockchainConfig).getOneAPL();
        doReturn("TPL").when(blockchainConfig).getCoinSymbol();

        validateSufficientlyWithEx(tx, "Transaction fee 1.000000 TPL less than minimum fee 2.000000 TPL at height 0");

        verify(attachment).performStateIndependentValidation(tx, 0);
    }

    @Test
    void validateSufficiently_notEnoughBalanceToPayFeeForReferencedTx() throws AplException.ValidationException {
        prepareFeeLightValidationScenario(14);

        validateSufficientlyWithEx(tx, "Account '1' balance 14 is not enough to pay tx fee 15");

        verify(attachment).performStateIndependentValidation(tx, 0);
    }

    @Test
    void validateSufficiently_signatureWasNotSet() throws AplException.ValidationException {
        prepareFeeLightValidationScenario(20);
        when(tx.getSignature()).thenReturn(null);
        when(tx.getStringId()).thenReturn("TestTX");

        validateSufficientlyWithEx(tx, "Invalid signature for transaction TestTX");

        verify(attachment).performStateIndependentValidation(tx, 0);
    }

    @Test
    void validateSufficiently_childTx_versionLessThan2() throws AplException.ValidationException {
        prepareFeeLightValidationScenario(20);
        when(tx.getStringId()).thenReturn("TestTX");
        when(tx.getVersion()).thenReturn((byte) 1);
        when(tx.getSignature()).thenReturn(mock(Signature.class));
        when(sender.isChild()).thenReturn(true);

        validateSufficientlyWithEx(tx, "Invalid signature for transaction TestTX");

        verify(attachment).performStateIndependentValidation(tx, 0);
    }


    private void prepareFeeLightValidationScenario(long senderBalance) {
        prepareLightValidationSuccessfulScenario();
        doReturn(new byte[32]).when(tx).referencedTransactionFullHash();
        doReturn(SENDER_ID).when(tx).getSenderId();
        doReturn(sender).when(accountService).getAccount(SENDER_ID);
        doReturn(10L).when(feeCalculator).getMinimumFeeATM(tx, 0);
        doReturn(5L).when(blockchainConfig).getUnconfirmedPoolDepositAtm();
        doReturn(senderBalance).when(sender).getUnconfirmedBalanceATM();
    }

    private void prepareLightValidationSuccessfulScenario() {
        prepareScenarioWithTxFields(1000, 10, 1440, 0);
        mockAttachment();
        doReturn(true).when(type).canHaveRecipient();
        doReturn(false).when(type).mustHaveRecipient();
        doReturn(true).when(attachment).verifyVersion();
        doReturn(List.of(attachment)).when(tx).getAppendages();
    }


    @Test
    void validateFully_OK() {
        Transaction transaction = createTransactionMocks();
        doReturn(UUID.randomUUID()).when(chain).getChainId();
        doReturn(10_000).when(heightConfig).getMaxPayloadLength();
        doReturn(1L).when(blockchain).getBlockIdAtHeight(100);

        validator.verifySignature(transaction);
        validator.validateFully(transaction);
    }

    @Test
    void validateFullyPhasingAtFinish() {
        Transaction transaction = td.TRANSACTION_13;
        doReturn(transaction.getHeight() + 1000).when(blockchain).getHeight();
        doReturn(true).when(transactionVersionValidator).isValidVersion(transaction);
        doReturn(300_000_000_000_000_000L).when(heightConfig).getMaxBalanceATM();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Account senderAcc = new Account(transaction.getSenderId(), transaction.getAmountATM() + transaction.getFeeATM(), transaction.getAmountATM() + transaction.getFeeATM(), 0, 0, 0);
        doReturn(senderAcc).when(accountService).getAccount(transaction.getSenderId());
        doReturn(UUID.randomUUID()).when(chain).getChainId();
        doReturn(10_000).when(heightConfig).getMaxPayloadLength();
        doReturn(1000).when(heightConfig).getMaxEncryptedMessageLength();
        doReturn(5629144656878115682L).when(blockchain).getBlockIdAtHeight(516746);
        doAnswer(invocation -> {
            if (invocation.getArgument(0).equals(transaction.getMessage())) {
                return new MessageAppendixValidator(blockchainConfig);
            } else if (invocation.getArgument(0).equals(transaction.getPhasing())) {
                return new PhasingAppendixValidator(blockchain, phasingPollService, blockchainConfig);
            } else if (invocation.getArgument(0).equals(transaction.getEncryptToSelfMessage())) {
                return new EncryptedToSelfMessageAppendixValidator(blockchainConfig);
            } else if (invocation.getArgument(0).equals(transaction.getPrunableEncryptedMessage())) {
                doReturn(transaction.getTimestamp() + 5000).when(timeService).getEpochTime();
                doReturn(1000).when(blockchainConfig).getMinPrunableLifetime();
                return new PrunableEncryptedMessageAppendixValidator(timeService, blockchainConfig);
            }
            else {
                return null;
            }
        }).when(validatorRegistry).getValidatorFor(any());
        doReturn(100).when(heightConfig).getMaxArbitraryMessageLength();


        validator.verifySignature(transaction);
        validator.validateFully(transaction);
    }

    private void mockAttachment() {
        doReturn(attachment).when(tx).getAttachment();
        doReturn(TransactionTypes.TransactionTypeSpec.ALIAS_BUY).when(attachment).getTransactionTypeSpec();
    }


    private Transaction createTransactionMocks() {
        Transaction transaction = createTransaction();
        doReturn(true).when(transactionVersionValidator).isValidVersion(transaction);
        doReturn(300_000_000_000_000_000L).when(heightConfig).getMaxBalanceATM();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        Account senderAcc = new Account(transaction.getSenderId(), transaction.getAmountATM() + transaction.getFeeATM(), transaction.getAmountATM() + transaction.getFeeATM(), 0, 0, 0);
        doReturn(senderAcc).when(accountService).getAccount(transaction.getSenderId());
        doReturn(true).when(accountPublicKeyService).setOrVerifyPublicKey(transaction.getSenderId(), transaction.getSenderPublicKey());
        doReturn(100).when(heightConfig).getMaxArbitraryMessageLength();
        doReturn(2000).when(blockchain).getHeight();
        doAnswer(invocation -> {
            if (invocation.getArgument(0).equals(transaction.getMessage())) {
                return new MessageAppendixValidator(blockchainConfig);
            } else if (invocation.getArgument(0).equals(transaction.getPhasing())) {
                return new PhasingAppendixValidator(blockchain, phasingPollService, blockchainConfig);
            } else {
                return null;
            }
        }).when(validatorRegistry).getValidatorFor(any());
        return transaction;
    }

    private HeightConfig mockMaxBalance() {
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(300_000_000_000_000_000L).when(heightConfig).getMaxBalanceATM();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        return heightConfig;
    }

    private void executeAndVerifyTxParametersError(int timestamp, long fee, int deadline, long amount) {
        validateSufficientlyWithEx(tx, createBadTxFieldsExMessage(timestamp, fee, deadline, amount));
    }

    private void validateSufficientlyWithEx(Transaction tx, String exMessage) {
        AplUnacceptableTransactionValidationException ex = assertThrows(AplUnacceptableTransactionValidationException.class,
            () -> validator.validateSufficiently(tx));

        assertEquals("Transaction '" + tx.toString() +"' failed with message: '" + exMessage + "'", ex.toString());
    }

    private String createBadTxFieldsExMessage(int timestamp, long fee, int deadline, long amount) {
        return "Invalid transaction parameters: " +
            "type: type, timestamp: " + timestamp + ", deadline: " + deadline + ", fee: "
            + fee + ", amount: " + amount;
    }

    private void prepareScenarioWithTxFields(int timestamp, long fee, int deadline, long amount) {
        mockTxType();
        mockMaxBalance();
        doReturn(true).when(transactionVersionValidator).isValidVersion(tx);
        doReturn(timestamp).when(tx).getTimestamp();
        doReturn(fee).when(tx).getFeeATM();
        doReturn((short)deadline).when(tx).getDeadline();
        doReturn(amount).when(tx).getAmountATM();
    }

    private void executeBadTxFieldsScenario(int timestamp, long fee, int deadline, long amount) {
        prepareScenarioWithTxFields(timestamp, fee, deadline, amount);
        executeAndVerifyTxParametersError(timestamp, fee, deadline, amount);
    }

    private void mockTxType() {
        doReturn(type).when(tx).getType();
        doReturn(TransactionTypes.TransactionTypeSpec.ALIAS_BUY).when(type).getSpec();
    }

    private Transaction createTransaction() {
        OrdinaryPaymentTransactionType type = new OrdinaryPaymentTransactionType(blockchainConfig, accountService);
        TransactionBuilderFactory builder = new TransactionBuilderFactory(new CachedTransactionTypeFactory(List.of(type)), blockchainConfig);
        TransactionSigner txSigner = new TransactionSignerImpl(blockchainConfig);
        TimeService timeService = mock(TimeService.class);
        int epochTime = (int) (System.currentTimeMillis() / 1000);
        doReturn(epochTime).when(timeService).getEpochTime();
        TransactionCreator txCreator = new TransactionCreator(validator, mock(PropertiesHolder.class), timeService, feeCalculator, blockchain, mock(TransactionProcessor.class), new CachedTransactionTypeFactory(List.of(type)), builder, txSigner, blockchainConfig);
        doReturn(new EcBlockData(1L, 100)).when(blockchain).getECBlock(epochTime);
        TransactionCreator.TransactionCreationData tx = txCreator.createTransaction(CreateTransactionRequest.builder()
            .secretPhrase("1")
            .publicKeyValue("39dc2e813bb45ff063a376e316b10cd0addd7306555ca0dd2890194d37960152")
            .validate(false)
            .broadcast(true)
            .deadlineValue(String.valueOf(1440))
            .attachment(new OrdinaryPaymentAttachment())
            .amountATM(500_000_000)
            .feeATM(100_000_000)
            .recipientId(Convert.parseAccountId("APL-NZKH-MZRE-2CTT-98NPZ"))
            .senderAccount(new Account(Convert.parseAccountId("APL-X5JH-TJKJ-DVGC-5T2V8"), 1000000000000000L, 10000000000000L, 0, 0, 0))
            .message(new MessageAppendix("Pub message"))
            .phased(true)
            .phasing(new PhasingAppendix(2323, new PhasingParams((byte) -1, 0, 0, 0, (byte) 0, new long[0]), new byte[0][], null, (byte) 0))
            .build());
        Transaction transaction = tx.getTx();
        return transaction;
    }
}