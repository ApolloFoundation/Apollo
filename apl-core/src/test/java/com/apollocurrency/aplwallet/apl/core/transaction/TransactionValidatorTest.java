/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
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
import com.apollocurrency.aplwallet.apl.core.transaction.messages.AppendixValidatorRegistry;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessageAppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.OrdinaryPaymentAttachment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendix;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PhasingAppendixValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.PrunableLoadingService;
import com.apollocurrency.aplwallet.apl.core.transaction.types.payment.OrdinaryPaymentTransactionType;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.data.TransactionTestData;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TransactionValidatorTest {
    BlockchainConfig blockchainConfig;
    @Mock PhasingPollService phasingPollService;
    @Mock Blockchain blockchain;
    @Mock FeeCalculator feeCalculator;
    @Mock AccountPublicKeyService accountPublicKeyService;
    @Mock AccountControlPhasingService accountControlPhasingService;
    @Mock PrunableLoadingService prunableService;
    @Mock AccountService accountService;
    @Mock TransactionVersionValidator transactionVersionValidator;
    @Mock AppendixValidatorRegistry validatorRegistry;

    @Mock
    Chain chain;
    @Mock
    HeightConfig heightConfig;
    TransactionTestData td;

    TransactionValidator validator;

    @BeforeEach
    void setUp() {
        td = new TransactionTestData();
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
        HeightConfig heightConfig = mock(HeightConfig.class);
        doReturn(300_000_000_000_000_000L).when(heightConfig).getMaxBalanceATM();
        doReturn(heightConfig).when(blockchainConfig).getCurrentConfig();
        doReturn(8).when(blockchainConfig).getDecimals();
        doReturn(30_000_000L).when(blockchainConfig).getInitialSupply();
        doReturn(100_000_000L).when(blockchainConfig).getOneAPL();

        validator.validateLightly(td.TRANSACTION_0);
    }

    @Test
    void validateSufficiently() {
        Transaction transaction = createTransaction();
        doReturn(true).when(transactionVersionValidator).isValidVersion(transaction);
        HeightConfig heightConfig = mock(HeightConfig.class);
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

        validator.validateSufficiently(transaction);
    }

    @Test
    void validateFully() {
        Transaction transaction = createTransactionMocks();
        doReturn(UUID.randomUUID()).when(chain).getChainId();
        doReturn(10_000).when(heightConfig).getMaxPayloadLength();
        doReturn(1L).when(blockchain).getBlockIdAtHeight(100);

        validator.verifySignature(transaction);
        validator.validateFully(transaction);
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

    private Transaction createTransaction() {
        OrdinaryPaymentTransactionType type = new OrdinaryPaymentTransactionType(blockchainConfig, accountService);
        TransactionBuilderFactory builder = new TransactionBuilderFactory(new CachedTransactionTypeFactory(List.of(type)), blockchainConfig);
        TransactionSigner txSigner = new TransactionSignerImpl(blockchainConfig);
        TimeService timeService = mock(TimeService.class);
        int epochTime = (int) (System.currentTimeMillis() / 1000);
        doReturn(epochTime).when(timeService).getEpochTime();
        TransactionCreator txCreator = new TransactionCreator(validator, mock(PropertiesHolder.class), timeService, feeCalculator, blockchain, mock(TransactionProcessor.class), new CachedTransactionTypeFactory(List.of(type)), builder, txSigner);
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