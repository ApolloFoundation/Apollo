/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.Shuffling;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingParticipant;
import com.apollocurrency.aplwallet.apl.core.entity.state.shuffling.ShufflingStage;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingRegistrationAttachment;
import com.apollocurrency.aplwallet.apl.crypto.Convert;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.SneakyThrows;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class ShufflingRegistrationTransactionTypeTest {
    private static final byte[] SHUFFLING_HASH = Convert.parseHexString("a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3");
    private final ShufflingRegistrationAttachment attachment = new ShufflingRegistrationAttachment(SHUFFLING_HASH);
    private static final long SHUFFLING_ID = -7120399779139131994L;
    private static final long SENDER_ID = 1L;
    private static final long ASSET_ID = 9999L;
    private Shuffling aplShuffling = new Shuffling(1000L, SHUFFLING_ID, 0, HoldingType.APL, 2L, 50, (byte) 3, (short) 1440, (byte) 2, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 100);
    private Shuffling assetShuffling = new Shuffling(1001L, SHUFFLING_ID, ASSET_ID, HoldingType.ASSET, 2L, 50, (byte) 3, (short) 1440, (byte) 2, ShufflingStage.REGISTRATION, 0, Convert.EMPTY_BYTES, 100);
    private Account senderAccount = new Account(SENDER_ID, 1000, 1000, 0, 0, 100);
    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    Blockchain blockchain;
    @Mock
    ShufflingService shufflingService;
    @Mock
    TransactionValidator validator;


    @Mock
    Transaction tx;

    @InjectMocks
    ShufflingRegistrationTransactionType type;


    private final AccountAssetService assetService = mock(AccountAssetService.class);

    @WeldSetup // required only for HoldingType.getUnconfirmedBalance methods
    WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(assetService, AccountAssetService.class))
        .build();


    @AfterEach
    void tearDown() {
        HoldingType.resetDependencies(); // reset after each container setup to guarantee no shared instances between tests
    }

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.SHUFFLING_REGISTRATION, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.SHUFFLING_REGISTRATION, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("ShufflingRegistration", type.getName());
    }

    @Test
    void parseAttachment_fromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(33);
        buff.put((byte) 1);
        buff.put(SHUFFLING_HASH);
        buff.flip();

        ShufflingRegistrationAttachment attachment = type.parseAttachment(buff);

        assertEquals(new ShufflingRegistrationAttachment(SHUFFLING_HASH), attachment);
    }

    @Test
    void parseAttachment_fromJson() {
        JSONObject json = new JSONObject();
        json.put("version.ShufflingRegistration", 1);
        json.put("shufflingFullHash", Convert.toHexString(SHUFFLING_HASH));

        ShufflingRegistrationAttachment attachment = type.parseAttachment(json);

        assertEquals(new ShufflingRegistrationAttachment(SHUFFLING_HASH), attachment);
    }

    @Test
    void doStateDependentValidation_noShuffling() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(null);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Shuffling not found: 11326344294570419622" , ex.getMessage());
    }

    @Test
    void doStateDependentValidation_noStateHash() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);
        when(shufflingService.getStageHash(aplShuffling)).thenReturn(null);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Shuffling state hash doesn't match" , ex.getMessage());
    }

    @Test
    void doStateDependentValidation_stateHashMismatch() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);
        when(shufflingService.getStageHash(aplShuffling)).thenReturn(new byte[32]);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Shuffling state hash doesn't match" , ex.getMessage());
    }

    @Test
    void doStateDependentValidation_shufflingStageMismatch() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);
        when(shufflingService.getStageHash(aplShuffling)).thenReturn(SHUFFLING_HASH);
        aplShuffling.setStage(ShufflingStage.DONE);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Shuffling registration has ended for 11326344294570419622" , ex.getMessage());
    }

    @Test
    void doStateDependentValidation_alreadyRegistered() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);
        when(shufflingService.getStageHash(aplShuffling)).thenReturn(SHUFFLING_HASH);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(shufflingService.getParticipant(SHUFFLING_ID, SENDER_ID)).thenReturn(mock(ShufflingParticipant.class));

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1 is already registered for shuffling 11326344294570419622" , ex.getMessage());
    }

    @Test
    void doStateDependentValidation_latePhasingRegistration() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);
        when(shufflingService.getStageHash(aplShuffling)).thenReturn(SHUFFLING_HASH);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(shufflingService.getParticipant(SHUFFLING_ID, SENDER_ID)).thenReturn(null);
        when(blockchain.getHeight()).thenReturn(2000);
        when(validator.getFinishValidationHeight(tx, attachment)).thenReturn(3500); // assume tx is phasing and execution height is at 3501 height

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Shuffling registration finishes in 1440 blocks" , ex.getMessage());
    }


    @SneakyThrows
    @Test
    void doStateDependentValidation_APL_OK() {
        mockCorrectStateDependentValidationFlow(aplShuffling);
        mockTxAmounts();

        type.doStateDependentValidation(tx);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_APL_NotEnoughFunds() {
        mockCorrectStateDependentValidationFlow(aplShuffling);
        mockTxAmounts();
        senderAccount.setUnconfirmedBalanceATM(69);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Sender 1 has not enough funds: required 70, but only has 69" , ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_ASSET_NotEnoughAssetFunds() {
        mockCorrectStateDependentValidationFlow(assetShuffling);
        when(assetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(49L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1 has not enough ASSET 9999 for shuffling registration: required 50, but has only 49" , ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_ASSET_NotEnoughAplFunds() {
        mockCorrectStateDependentValidationFlow(assetShuffling);
        when(assetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(50L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(981L);
        mockTxAmounts();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Sender 1 has not enough funds: required 1001, but only has 1000" , ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_ASSET_OK() {
        mockCorrectStateDependentValidationFlow(assetShuffling);
        when(assetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(50L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(980L);
        mockTxAmounts();

        type.doStateDependentValidation(tx);
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_OK() {
        type.doStateIndependentValidation(tx);
    }

    @Test
    void isDuplicate_noRegistrationTxs() {
        mockTxAttachmentAndSender();
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertFalse(duplicate, "Shuffling registration tx should not be a duplicate," +
            " when no same txs exist in the block");
    }

    @Test
    void isDuplicate_oneRegistrationTxInTheBlockFromTheSameSender() {
        mockTxAttachmentAndSender();

        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        duplicates.put(TransactionTypes.TransactionTypeSpec.SHUFFLING_REGISTRATION, Map.of(Long.toUnsignedString(SHUFFLING_ID) + "." + SENDER_ID, 0));
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertTrue(duplicate, "Second shuffling registration tx from the same sender should be a duplicate");
    }

    @Test
    void isDuplicate_oneRegistrationTxFromAnotherSenderForSameShuffling() {
        mockTxAttachmentAndSender();

        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        duplicates.put(TransactionTypes.TransactionTypeSpec.SHUFFLING_REGISTRATION, new HashMap<>(Map.of(Long.toUnsignedString(SHUFFLING_ID), 1)));
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertTrue(duplicate, "Second shuffling registration tx for the same shuffling be a duplicate");
    }

    @Test
    void applyAttachmentUnconfirmed_APL_OK() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertTrue(applied, "applyAttachmentUnconfirmed should pass when account has enough APLs");
        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, -50L);
    }

    @Test
    void applyAttachmentUnconfirmed_APL_NotEnoughFunds() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);
        senderAccount.setUnconfirmedBalanceATM(49);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertFalse(applied, "applyAttachmentUnconfirmed should NOT pass when account has not enough APLs on unconfirmed balance");
    }


    @Test
    void applyAttachmentUnconfirmed_ASSET_OK() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(assetShuffling);
        when(assetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(50L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(900L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertTrue(applied, "applyAttachmentUnconfirmed must pass when account has enough APLs and Asset on unconfirmed balance");
        verify(assetService).addToUnconfirmedAssetBalanceATU(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0L, ASSET_ID, -50);
        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0L, -900);
    }

    @Test
    void applyAttachmentUnconfirmed_ASSET_NotEnoughAssetBalance() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(assetShuffling);
        when(assetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(49L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertFalse(applied, "applyAttachmentUnconfirmed must NOT pass when account has not enough Asset on unconfirmed balance");
        verifyNoInteractions(accountService);
        verify(assetService, never()).addToUnconfirmedAssetBalanceATU(any(), any(), anyLong(), anyLong(), anyLong());
    }

    @Test
    void applyAttachmentUnconfirmed_ASSET_NotEnoughAPLBalance() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(assetShuffling);
        when(assetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(50L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(1001L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertFalse(applied, "applyAttachmentUnconfirmed must NOT pass when account has not enough APL on unconfirmed balance (shufflingDepositAtm)");
    }

    @Test
    void applyAttachment() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);
        when(tx.getSenderId()).thenReturn(SENDER_ID);

        type.apply(tx, senderAccount, null);

        verify(shufflingService).addParticipant(aplShuffling, SENDER_ID);
    }

    @Test
    void undoAttachmentUnconfirmed_ASSET() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(assetShuffling);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(100L);

        type.undoAttachmentUnconfirmed(tx, senderAccount);

        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, 100);
        verify(assetService).addToUnconfirmedAssetBalanceATU(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, ASSET_ID, 50);
    }

    @Test
    void undoAttachmentUnconfirmed_APL() {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(aplShuffling);

        type.undoAttachmentUnconfirmed(tx, senderAccount);

        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, 50);
    }

    private void mockAttachment() {
        when(tx.getAttachment()).thenReturn(attachment);
    }

    private void mockTxAttachmentAndSender() {
        mockAttachment();
        when(tx.getSenderId()).thenReturn(SENDER_ID);
    }

    private void mockTxAmounts() {
        when(tx.getFeeATM()).thenReturn(20L);
        when(tx.getAmountATM()).thenReturn(0L);
    }

    private void mockCorrectStateDependentValidationFlow(Shuffling shuffling) {
        mockAttachment();
        when(shufflingService.getShuffling(SHUFFLING_ID)).thenReturn(shuffling);
        when(shufflingService.getStageHash(shuffling)).thenReturn(SHUFFLING_HASH);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(shufflingService.getParticipant(SHUFFLING_ID, SENDER_ID)).thenReturn(null);
        when(blockchain.getHeight()).thenReturn(2000);
        when(validator.getFinishValidationHeight(tx, attachment)).thenReturn(2000);
        when(accountService.getAccount(SENDER_ID)).thenReturn(senderAccount);
    }
}