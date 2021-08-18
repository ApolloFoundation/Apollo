/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.entity.state.asset.Asset;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountAssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetDividendService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionValidator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCDividendPaymentAttachment;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
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
    void doStateDependentValidation() {

    }

    @Test
    void doStateIndependentValidation() {
    }

    @Test
    void isDuplicate() {
    }

    @Test
    void canHaveRecipient() {
    }

    @Test
    void isPhasingSafe() {
    }

    private CCDividendPaymentAttachment mockAttachment(int height, long rate) {
        CCDividendPaymentAttachment attachment = new CCDividendPaymentAttachment(ASSET_ID, height, rate);
        doReturn(attachment).when(tx).getAttachment();
        return attachment;
    }
}