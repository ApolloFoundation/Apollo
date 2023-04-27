/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.shuffling;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.HeightConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.currency.Currency;
import com.apollocurrency.aplwallet.apl.core.model.HoldingType;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.service.state.ShufflingService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountCurrencyService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.currency.CurrencyService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.ShufflingCreationAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import lombok.SneakyThrows;
import org.jboss.weld.junit.MockBean;
import org.jboss.weld.junit5.EnableWeld;
import org.jboss.weld.junit5.WeldInitiator;
import org.jboss.weld.junit5.WeldSetup;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@EnableWeld
@ExtendWith(MockitoExtension.class)
class ShufflingCreationTransactionTypeTest {

    private static final long SENDER_ID = 1L;
    private static final long ASSET_ID = 9999L;
    private static final long CURRENCY_ID = 8999L;
    private final ShufflingCreationAttachment assetAttachment = new ShufflingCreationAttachment(ASSET_ID, HoldingType.ASSET, 100, (byte) 3, (short) 1440);
    private final ShufflingCreationAttachment aplAttachment = new ShufflingCreationAttachment(0, HoldingType.APL, 100, (byte) 3, (short) 1440);
    private final ShufflingCreationAttachment currencyAttachment = new ShufflingCreationAttachment(CURRENCY_ID, HoldingType.CURRENCY, 100, (byte) 3, (short) 1440);


    private final ShufflingCreationAttachment incorrectAmountAssetAttachment = new ShufflingCreationAttachment(ASSET_ID, HoldingType.ASSET, 0, (byte) 3, (short) 1440);
    private final ShufflingCreationAttachment incorrectAmountCurrencyAttachment = new ShufflingCreationAttachment(CURRENCY_ID, HoldingType.CURRENCY, 0, (byte) 3, (short) 1440);
    private final ShufflingCreationAttachment notEnoughParticipantAttachment = new ShufflingCreationAttachment(0, HoldingType.APL, 100, (byte) 2, (short) 1440);
    private final ShufflingCreationAttachment tooManyParticipantAttachment = new ShufflingCreationAttachment(0, HoldingType.APL, 100, (byte) 31, (short) 1440);
    private final ShufflingCreationAttachment zeroRegistrationPeriodAttachment = new ShufflingCreationAttachment(0, HoldingType.APL, 100, (byte) 3, (short) 0);
    private final ShufflingCreationAttachment tooBigRegistrationPeriodAttachment = new ShufflingCreationAttachment(0, HoldingType.APL, 100, (byte) 3, (short) Short.MAX_VALUE);

    private Account senderAccount = new Account(SENDER_ID, 1000, 1000, 0, 0, 100);

    @Mock
    AccountService accountService;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AssetService assetService;
    @Mock
    CurrencyService currencyService;
    @Mock
    ShufflingService shufflingService;
    @Mock
    Transaction tx;
    @Mock
    Asset asset;
    @Mock
    Currency currency;
    @Mock
    HeightConfig heightConfig;

    @InjectMocks
    ShufflingCreationTransactionType type;

    private final AccountAssetService accountAssetService = mock(AccountAssetService.class);
    private final AccountCurrencyService accountCurrencyService = mock(AccountCurrencyService.class);

    @WeldSetup // required only for HoldingType.getUnconfirmedBalance methods
    WeldInitiator weld = WeldInitiator.from()
        .addBeans(MockBean.of(accountAssetService, AccountAssetService.class))
        .addBeans(MockBean.of(accountCurrencyService, AccountCurrencyService.class))
        .build();

    private static MockedStatic<HoldingType> myMockedEnum;
    private static HoldingType mockedValue;

    @BeforeClass
    public void setUp() {
        // here we want to create impossible mocked ENUM value for one tests only
        HoldingType[] newEnumValues = addNewEnumValue(HoldingType.class);
        myMockedEnum = mockStatic(HoldingType.class);
        myMockedEnum.when(HoldingType::values).thenReturn(newEnumValues);
        mockedValue = newEnumValues[newEnumValues.length - 1]; // value will be used in one test method below
    }

    private static <E extends Enum<E>> E[] addNewEnumValue(Class<E> enumClazz){
        EnumSet<E> enumSet = EnumSet.allOf(enumClazz);
        E[] newValues = (E[]) Array.newInstance(enumClazz, enumSet.size() + 1);
        int i = 0;
        for (E value : enumSet) {
            newValues[i] = value;
            i++;
        }

        E newEnumValue = mock(enumClazz);
        newValues[newValues.length - 1] = newEnumValue;

        when(newEnumValue.ordinal()).thenReturn(newValues.length - 1);

        return newValues;
    }

    @AfterClass
    public void tearDownAll(){
        myMockedEnum.close(); // IMPORTANT CLEAN UP in order to do not affect other tests !!!
    }

    @AfterEach
    void tearDown() {
        HoldingType.resetDependencies(); // reset after each container setup to guarantee no shared instances between tests
    }

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.SHUFFLING_CREATION, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.SHUFFLING_REGISTRATION, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("ShufflingCreation", type.getName());
    }

    @Test
    void parseAttachment_fromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(21);
        buff.put((byte) 1);
        buff.putLong(ASSET_ID);
        buff.put((byte) 1);
        buff.putLong(100L);
        buff.put((byte) 3);
        buff.putShort((short) 1440);
        assertFalse(buff.hasRemaining(), "Shuffling creation attachment should consist of 21 bytes");
        buff.flip();

        ShufflingCreationAttachment attachment = type.parseAttachment(buff);

        assertEquals(assetAttachment, attachment);
    }

    @Test
    void parseAttachment_fromJson() {
        JSONObject json = new JSONObject();
        json.put("version.ShufflingCreation", 1);
        json.put("holding", Long.toUnsignedString(assetAttachment.getHoldingId()));
        json.put("holdingType", assetAttachment.getHoldingType().getCode());
        json.put("amount", assetAttachment.getAmount());
        json.put("participantCount", assetAttachment.getParticipantCount());
        json.put("registrationPeriod", assetAttachment.getRegistrationPeriod());

        ShufflingCreationAttachment attachment = type.parseAttachment(json);

        assertEquals(assetAttachment, attachment);
    }

    @Test
    void doStateDependentValidation_noAsset() {
        when(tx.getAttachment()).thenReturn(assetAttachment);
        when(assetService.getAsset(ASSET_ID)).thenReturn(null);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Unknown asset 9999", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_shufflingAmountExceedAssetQuantity() {
        when(tx.getAttachment()).thenReturn(assetAttachment);
        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(10L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Invalid asset quantity 100", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_ASSET_notEnoughAssetFunds() {
        when(tx.getAttachment()).thenReturn(assetAttachment);
        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(1000L);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(senderAccount);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(10L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Account 1 has not enough ASSET 9999 for shuffling creation: required 100, " +
            "but has only 10", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_ASSET_notEnoughAPLFunds() {
        when(tx.getAttachment()).thenReturn(assetAttachment);
        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(1000L);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(senderAccount);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(100L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(100L);
        when(tx.getFeeATM()).thenReturn(1000L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Sender 1 has not enough funds: required 1100, but only has 1000", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_ASSET_OK() {
        when(tx.getAttachment()).thenReturn(assetAttachment);
        when(assetService.getAsset(ASSET_ID)).thenReturn(asset);
        when(asset.getInitialQuantityATU()).thenReturn(1000L);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(senderAccount);
        when(accountAssetService.getUnconfirmedAssetBalanceATU(SENDER_ID, ASSET_ID)).thenReturn(100L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(100L);
        when(tx.getFeeATM()).thenReturn(900L);

        type.doStateDependentValidation(tx);
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_APL_OK() {
        when(tx.getAttachment()).thenReturn(aplAttachment);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(senderAccount);
        when(tx.getFeeATM()).thenReturn(900L);

        type.doStateDependentValidation(tx);
    }

    @Test
    void doStateDependentValidation_APL_NotEnoughFunds() {
        when(tx.getAttachment()).thenReturn(aplAttachment);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(senderAccount);
        when(tx.getFeeATM()).thenReturn(1000L);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Sender 1 has not enough funds: required 1100, but only has 1000", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_CURRENCY_currencyIsNotActive() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currency.getCode()).thenReturn("TEST_CODE");
        when(currencyService.isActive(currency)).thenReturn(false);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateDependentValidation(tx));

        assertEquals("Currency is not active: TEST_CODE", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateDependentValidation_CURRENCY_OK() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currencyService.isActive(currency)).thenReturn(true);
        when(tx.getSenderId()).thenReturn(SENDER_ID);
        when(accountService.getAccount(SENDER_ID)).thenReturn(senderAccount);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, CURRENCY_ID)).thenReturn(100L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(100L);
        when(tx.getFeeATM()).thenReturn(900L);

        type.doStateDependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_APL_tooSmallShufflingAmount() {
        when(tx.getAttachment()).thenReturn(aplAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(120L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid ATM amount 100, minimum is 120", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_APL_tooBigShufflingAmount() {
        when(tx.getAttachment()).thenReturn(aplAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(10L);

        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(99L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid ATM amount 100, minimum is 10", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_APL_OK() {
        when(tx.getAttachment()).thenReturn(aplAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(10L);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(1000L);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_ASSET_zeroAmount() {
        when(tx.getAttachment()).thenReturn(incorrectAmountAssetAttachment);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid asset quantity 0", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_ASSET_OK() {
        when(tx.getAttachment()).thenReturn(assetAttachment);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_CURRENCY_zeroAmount() {
        when(tx.getAttachment()).thenReturn(incorrectAmountCurrencyAttachment);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid currency amount 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_CURRENCY_maxAPlAmountExceeded() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(blockchainConfig.getOneAPL()).thenReturn(1L);
        when(blockchainConfig.getInitialSupply()).thenReturn(10L);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid currency amount 100", ex.getMessage());
    }

    @SneakyThrows
    @Test
    void doStateIndependentValidation_CURRENCY_OK() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(blockchainConfig.getOneAPL()).thenReturn(1L);
        when(blockchainConfig.getInitialSupply()).thenReturn(1000L);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_unknownHoldingType() {
        // YL: it's not easy to create ENUM mock by mockito now
        HoldingType incorrectHoldingType = mockedValue; // fails at run-time
        ShufflingCreationAttachment incorrectHoldingAttachment =
            new ShufflingCreationAttachment(ASSET_ID, incorrectHoldingType, 100, (byte) 3, (short) 1440);

        when(tx.getAttachment()).thenReturn(incorrectHoldingAttachment);

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> type.doStateIndependentValidation(tx));

        assertTrue(ex.getMessage().startsWith("Unsupported holding type null"), "Unknown holding type should error message changed");
    }

    @Test
    void doStateIndependentValidation_notEnoughParticipant() {
        when(tx.getAttachment()).thenReturn(notEnoughParticipantAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(10L);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(1000L);

        AplException.NotValidException ex = assertThrows( AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Number of participants 2 is not between 3 and 30", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_tooManyParticipants() {
        when(tx.getAttachment()).thenReturn(tooManyParticipantAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(10L);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(1000L);

        AplException.NotValidException ex = assertThrows( AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Number of participants 31 is not between 3 and 30", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_zeroRegistrationPeriod() {
        when(tx.getAttachment()).thenReturn(zeroRegistrationPeriodAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(10L);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(1000L);

        AplException.NotValidException ex = assertThrows( AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid registration period: 0", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_tooBigRegistrationPeriod() {
        when(tx.getAttachment()).thenReturn(tooBigRegistrationPeriodAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(10L);
        when(blockchainConfig.getCurrentConfig()).thenReturn(heightConfig);
        when(heightConfig.getMaxBalanceATM()).thenReturn(1000L);

        AplException.NotValidException ex = assertThrows( AplException.NotValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid registration period: 32767", ex.getMessage());
    }

    @Test
    void applyAttachmentUnconfirmed() {

    }


    @Test
    void applyAttachmentUnconfirmed_APL_OK() {
        when(tx.getAttachment()).thenReturn(aplAttachment);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertTrue(applied, "applyAttachmentUnconfirmed should pass when account has enough APLs");
        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, -100L);
    }

    @Test
    void applyAttachmentUnconfirmed_APL_NotEnoughFunds() {
        when(tx.getAttachment()).thenReturn(aplAttachment);
        senderAccount.setUnconfirmedBalanceATM(99);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertFalse(applied, "applyAttachmentUnconfirmed should NOT pass when account has not enough APLs on unconfirmed balance");
    }


    @Test
    void applyAttachmentUnconfirmed_CURRENCY_NotEnoughCurrencyFunds() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, CURRENCY_ID)).thenReturn(10L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertFalse(applied, "applyAttachmentUnconfirmed must NOT pass when account has NOT enough currency on unconfirmed balance");
    }

    @Test
    void applyAttachmentUnconfirmed_CURRENCY_NotEnoughAplFunds() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, CURRENCY_ID)).thenReturn(100L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(1200L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertFalse(applied, "applyAttachmentUnconfirmed must NOT pass when account has NOT enough apl on unconfirmed balance");
    }

    @Test
    void applyAttachmentUnconfirmed_CURRENCY_OK() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(accountCurrencyService.getUnconfirmedCurrencyUnits(senderAccount, CURRENCY_ID)).thenReturn(100L);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(900L);

        boolean applied = type.applyAttachmentUnconfirmed(tx, senderAccount);

        assertTrue(applied, "applyAttachmentUnconfirmed must pass when account has  enough currency and apl on unconfirmed balance");
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, CURRENCY_ID, -100);
        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, -900);
    }

    @Test
    void applyAttachment() {
        when(tx.getAttachment()).thenReturn(aplAttachment);

        type.applyAttachment(tx, senderAccount, null);

        verify(shufflingService).addShuffling(tx, aplAttachment);
    }

    @Test
    void undoAttachmentUnconfirmed_Currency() {
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(blockchainConfig.getShufflingDepositAtm()).thenReturn(900L);

        type.undoAttachmentUnconfirmed(tx, senderAccount);

        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, 900);
        verify(accountCurrencyService).addToUnconfirmedCurrencyUnits(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, CURRENCY_ID, 100);
    }

    @Test
    void undoAttachmentUnconfirmed_APL() {
        when(tx.getAttachment()).thenReturn(aplAttachment);

        type.undoAttachmentUnconfirmed(tx, senderAccount);

        verify(accountService).addToUnconfirmedBalanceATM(senderAccount, LedgerEvent.SHUFFLING_REGISTRATION, 0, 100);
    }

    @Test
    void isDuplicate_noDuplicatesForApl() {
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        when(tx.getAttachment()).thenReturn(aplAttachment);

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertFalse(duplicate, "APL shuffling creation tx should never be a duplicate ");
    }
    @Test
    void isDuplicate_noDuplicatesForAsset() {
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        when(tx.getAttachment()).thenReturn(assetAttachment);

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertFalse(duplicate, "Asset shuffling creation tx should never be a duplicate ");
    }

    @Test
    void isDuplicate_currencyIsNotDuplicate() {
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currency.getCode()).thenReturn("TST");
        when(currency.getName()).thenReturn("TEST CURRENCY");

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertFalse(duplicate, "Currency shuffling creation tx should not be a duplicate ");
    }

    @Test
    void isDuplicate_currencyIsDuplicateByCode_currencyDeletionTxPresent() {
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        // simulate currency delete tx
        duplicates.put(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE, new HashMap<>(Map.of("tst", 0)));

        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currency.getCode()).thenReturn("TST");
        when(currency.getName()).thenReturn("TEST CURRENCY");

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertTrue(duplicate, "Currency shuffling creation tx should be a duplicate, when currency delete tx is present");
    }

    @Test
    void isDuplicate_currencyIsDuplicateByName_currencyDeletionTxPresent() {
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        // simulate currency delete tx
        duplicates.put(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE, new HashMap<>(Map.of("test currency", 0)));

        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currency.getCode()).thenReturn("TST");
        when(currency.getName()).thenReturn("TEST CURRENCY");

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertTrue(duplicate, "Currency shuffling creation tx should be a duplicate, when currency delete tx is present");
    }

    @Test
    void isDuplicate_currencyIsDuplicateByName_nameAndCodeAreEquals_currencyDeletionTxPresent() {
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicates = new HashMap<>();
        // simulate currency delete tx
        duplicates.put(TransactionTypes.TransactionTypeSpec.MS_CURRENCY_ISSUANCE, new HashMap<>(Map.of("tst", 0)));

        when(tx.getAttachment()).thenReturn(currencyAttachment);
        when(currencyService.getCurrency(CURRENCY_ID)).thenReturn(currency);
        when(currency.getCode()).thenReturn("TST");
        when(currency.getName()).thenReturn("TST");

        boolean duplicate = type.isDuplicate(tx, duplicates);

        assertTrue(duplicate, "Currency shuffling creation tx should be a duplicate, when currency delete tx is present");
    }
}