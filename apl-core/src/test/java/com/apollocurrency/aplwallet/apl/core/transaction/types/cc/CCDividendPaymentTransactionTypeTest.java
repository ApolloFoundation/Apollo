/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.AssetDividend;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDividendService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCDividendPaymentAttachment;
import com.apollocurrency.aplwallet.apl.util.exception.AplException;
import org.json.simple.JSONObject;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CCDividendPaymentTransactionTypeTest {
    public static final long ASSET_ID = 1L;
    @Mock
    Blockchain blockchain;
    @Mock
    BlockchainConfig blockchainConfig;
    @Mock
    AssetService assetService;
    @Mock
    AccountAssetService accountAssetService;
    @Mock
    AssetDividendService assetDividendService;
    @Mock
    TransactionValidator validator;
    @Mock
    AccountService accountService;

    @InjectMocks
    CCDividendPaymentTransactionType type;

    // supporting mocks for scenarios
    @Mock
    Transaction tx;
    @Mock
    Account sender;
    @Mock
    Asset asset;

    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.CC_DIVIDEND_PAYMENT, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.ASSET_DIVIDEND_PAYMENT, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("DividendPayment", type.getName());
    }

    @Test
    void parseAttachmentFromBytes() {
        ByteBuffer buff = ByteBuffer.allocate(21);
        buff.put((byte) 1);
        buff.putLong(ASSET_ID);
        buff.putInt(1000);
        buff.putLong(10);
        buff.flip();

        CCDividendPaymentAttachment attachment = type.parseAttachment(buff);

        assertEquals(new CCDividendPaymentAttachment(ASSET_ID, 1000, 10), attachment);
    }

    @Test
    void parseAttachmentFromJson() {
        JSONObject json = new JSONObject();
        json.put("version.DividendPayment", 1L);
        json.put("asset", "1");
        json.put("height", 1000L);
        json.put("amountATMPerATU", 10L);

        CCDividendPaymentAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(new CCDividendPaymentAttachment(ASSET_ID, 1000, 10), parsedAttachment);
    }

    @Test
    void applyAttachmentUnconfirmed_noAssetAtHeight() {
        mockAttachment(500, 2);
        doReturn(null).when(assetService).getAsset(ASSET_ID, 500);

        boolean result = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(result, "Dividend payment applyAttachmentUnconfirmed should be successful, " +
            "when asset is not available - no dividend payments will be done");
        verifyNoInteractions(accountService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        mockAttachment(500, 2);
        doReturn(asset).when(assetService).getAsset(ASSET_ID, 500);
        doReturn(2_000L).when(asset).getQuantityATU();
        doReturn(5_000L).when(sender).getUnconfirmedBalanceATM();
        doReturn(1200L).when(accountAssetService).getAssetBalanceATU(sender, ASSET_ID, 500);

        boolean result = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(result, "Dividend payment applyAttachmentUnconfirmed should be successful, " +
            "when total dividend payment for asset '1' is 1600 and account has balance of 5000");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.ASSET_DIVIDEND_PAYMENT, 0, -1600);
    }


    @Test
    void applyAttachmentUnconfirmed_DoubleSpending() {
        mockAttachment(500, 2);
        doReturn(asset).when(assetService).getAsset(ASSET_ID, 500);
        doReturn(2_000L).when(asset).getQuantityATU();
        doReturn(3_500L).when(sender).getUnconfirmedBalanceATM();
        doReturn(200L).when(accountAssetService).getAssetBalanceATU(sender, ASSET_ID, 500);

        boolean result = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(result, "Dividend payment applyAttachmentUnconfirmed should NOT be successful, " +
            "when total dividend payment for asset '1' is 3600 and account has not enough balance, only 3500");
        verifyNoMoreInteractions(accountService);
    }

    @Test
    void applyAttachment() {
        CCDividendPaymentAttachment attachment = mockAttachment(1000, 5);

        type.applyAttachment(tx, sender, null);

        verify(accountAssetService).payDividends(sender, 0, attachment);
    }

    @Test
    void undoAttachmentUnconfirmed_noAsset() {
        mockAttachment(1000, 5);
        doReturn(null).when(assetService).getAsset(ASSET_ID, 1000);

        type.undoAttachmentUnconfirmed(tx, sender);

        verifyNoInteractions(accountService, accountAssetService);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        mockAttachment(1000, 5);
        doReturn(asset).when(assetService).getAsset(ASSET_ID, 1000);
        doReturn(1000L).when(asset).getQuantityATU();
        doReturn(200L).when(accountAssetService).getAssetBalanceATU(sender, ASSET_ID, 1000);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.ASSET_DIVIDEND_PAYMENT, 0, 4000);
    }

    @Test
    void doStateDependentValidation_noAsset() {
        mockAttachment(1000, 5);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doStateDependentValidation(tx));

        assertEquals("Asset 1 for dividend payment doesn't exist yet", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_txSenderIsNotAssetsCreator() {
        mockAttachment(1000, 5);
        doReturn(asset).when(assetService).getAsset(ASSET_ID, 1000);
        doReturn(44L).when(asset).getAccountId();
        doReturn(54L).when(tx).getSenderId();

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateDependentValidation(tx));

        assertEquals("Invalid dividend payment sender {\"version.DividendPayment\":1,\"asset\":\"1\"," +
            "\"amountATMPerATU\":5,\"height\":1000}", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_lastDividendWasNotSoLongAgo() {
        mockAttachment(1000, 2);
        doReturn(asset).when(assetService).getAsset(ASSET_ID, 1000);
        doReturn(44L).when(asset).getAccountId();
        doReturn(44L).when(tx).getSenderId();
        AssetDividend dividend = mock(AssetDividend.class);
        doReturn(dividend).when(assetDividendService).getLastDividend(ASSET_ID);
        doReturn(1020).when(dividend).getHeight();
        doReturn(1079).when(blockchain).getHeight();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class, () -> type.doStateDependentValidation(tx));

        assertEquals("Last dividend payment for asset 1 was less than 60 blocks ago at 1020, " +
            "current height is 1079, limit is one dividend per 60 blocks", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_lastDividendWasMoreThan60BlocksAgo_amountToPayOverflow() {
        mockAttachment(1000, Long.MAX_VALUE / 2999);
        doReturn(44L).when(asset).getAccountId();
        doReturn(4000L).when(asset).getQuantityATU();
        doReturn(1L).when(asset).getId();
        doReturn(asset).when(assetService).getAsset(ASSET_ID, 1000);
        doReturn(44L).when(tx).getSenderId();
        doReturn(type).when(tx).getType();
        AssetDividend dividend = mock(AssetDividend.class);
        doReturn(dividend).when(assetDividendService).getLastDividend(ASSET_ID);
        doReturn(1020).when(dividend).getHeight();
        doReturn(1081).when(blockchain).getHeight();
        doReturn(1000L).when(accountAssetService).getAssetBalanceATU(44L, ASSET_ID, 1000);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateDependentValidation(tx));

        assertEquals("Result of multiplying x=3075482506453743, y=3000 exceeds the allowed range " +
            "[-9223372036854775808;9223372036854775807], transaction='null', type='CC_DIVIDEND_PAYMENT', sender='44'", ex.getMessage());
    }

    @Test
    void doStateDependentValidation_noLastDividend_OK() throws AplException.ValidationException {
        mockAttachment(1000, 5);
        doReturn(44L).when(asset).getAccountId();
        doReturn(4000L).when(asset).getQuantityATU();
        doReturn(1L).when(asset).getId();
        doReturn(asset).when(assetService).getAsset(ASSET_ID, 1000);
        doReturn(44L).when(tx).getSenderId();
        doReturn(type).when(tx).getType();
        doReturn(1000L).when(accountAssetService).getAssetBalanceATU(44L, ASSET_ID, 1000);

        type.doStateDependentValidation(tx);
    }

    @Test
    void doStateIndependentValidation_zeroRate() {
        mockAttachment(2000, 0);

        AplException.NotValidException ex = assertThrows(AplException.NotValidException.class, () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid dividend payment amount {\"version.DividendPayment\":1,\"asset\":\"1\"," +
            "\"amountATMPerATU\":0,\"height\":2000}", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_dividendPaymentHeightGreaterThanBlockchainHeight() {
        mockAttachment(2000, 2);
        doReturn(1999).when(blockchain).getHeight();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid dividend payment height: 2000, must not exceed current blockchain height 1999", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_() {
        mockAttachment(2000, 2);
        doReturn(1999).when(blockchain).getHeight();

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid dividend payment height: 2000, must not exceed current blockchain height 1999", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_tooDeepAssetDividendPayment_forPhasing() {
        CCDividendPaymentAttachment attachment = mockAttachment(2000, 2);
        doReturn(2100).when(blockchain).getHeight();
        // phased transaction will be executed at the 3500 height
        doReturn(3500).when(validator).getFinishValidationHeight(tx, attachment);

        AplException.NotCurrentlyValidException ex = assertThrows(AplException.NotCurrentlyValidException.class,
            () -> type.doStateIndependentValidation(tx));

        assertEquals("Invalid dividend payment height: 2000, must be less than 1441 blocks before 3500", ex.getMessage());
    }

    @Test
    void doStateIndependentValidation_OK_forPhasing() throws AplException.ValidationException {
        CCDividendPaymentAttachment attachment = mockAttachment(2000, 2);
        doReturn(2100).when(blockchain).getHeight();
        // phased transaction will be executed at the 3400 height, which less than 1441 blocks above dividend payment height
        // 2000 and asset historical holder's data at 2000 height will be available
        doReturn(3400).when(validator).getFinishValidationHeight(tx, attachment);

        type.doStateIndependentValidation(tx);
    }

    @Test
    void isDuplicate() {
        mockAttachment(2000, 5);
        HashMap<TransactionTypes.TransactionTypeSpec, Map<String, Integer>> duplicatesMap = new HashMap<>();

        boolean duplicate = type.isDuplicate(tx, duplicatesMap);

        assertFalse(duplicate, "Empty duplicates map should allow DIVIDEND_PAYMENT tx type to not be duplicate");

        boolean duplicateAfterDividendPaymentIncluded = type.isDuplicate(tx, duplicatesMap);

        assertTrue(duplicateAfterDividendPaymentIncluded, "Duplicates check should not allow two DIVIDEND_PAYMENT transactions for asset '1'");
    }

    @Test
    void canHaveRecipient() {
        assertFalse(type.canHaveRecipient(), "DIVIDEND_PAYMENT should not allow recipients");
    }

    @Test
    void isPhasingSafe() {
        assertFalse(type.isPhasingSafe(), "DIVIDEND_PAYMENT should not be phasing safe, but may be phasing");
    }

    private CCDividendPaymentAttachment mockAttachment(int height, long rate) {
        CCDividendPaymentAttachment attachment = new CCDividendPaymentAttachment(ASSET_ID, height, rate);
        doReturn(attachment).when(tx).getAttachment();
        return attachment;
    }
}