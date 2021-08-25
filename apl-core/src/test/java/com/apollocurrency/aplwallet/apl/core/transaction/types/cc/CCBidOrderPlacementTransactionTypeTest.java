/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.transaction.types.cc;

import com.apollocurrency.aplwallet.apl.core.app.AplException;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.Account;
import com.apollocurrency.aplwallet.apl.core.entity.state.account.LedgerEvent;
import com.apollocurrency.aplwallet.apl.core.service.state.account.AccountService;
import com.apollocurrency.aplwallet.apl.core.service.state.asset.AssetService;
import com.apollocurrency.aplwallet.apl.core.service.state.order.OrderMatchService;
import com.apollocurrency.aplwallet.apl.core.transaction.TransactionTypes;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.CCBidOrderPlacementAttachment;
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

@ExtendWith(MockitoExtension.class)
class CCBidOrderPlacementTransactionTypeTest {
    public static final long ASSET_ID = 1L;
    @Mock
    BlockchainConfig config;
    @Mock
    AccountService accountService;
    @Mock
    AssetService assetService;
    @Mock
    OrderMatchService orderMatchService;

    @InjectMocks
    CCBidOrderPlacementTransactionType type;

    // supporting mocks
    @Mock
    Transaction tx;
    @Mock
    Account sender;


    @Test
    void getSpec() {
        assertEquals(TransactionTypes.TransactionTypeSpec.CC_BID_ORDER_PLACEMENT, type.getSpec());
    }

    @Test
    void getLedgerEvent() {
        assertEquals(LedgerEvent.ASSET_BID_ORDER_PLACEMENT, type.getLedgerEvent());
    }

    @Test
    void getName() {
        assertEquals("BidOrderPlacement", type.getName());
    }

    @Test
    void parseAttachmentFromBytes() throws AplException.NotValidException {
        ByteBuffer buff = ByteBuffer.allocate(25);
        buff.put((byte) 1); // version
        buff.putLong(ASSET_ID); //asset
        buff.putLong(100); // quantityATU
        buff.putLong(2); // priceATM
        buff.flip();

        CCBidOrderPlacementAttachment parsedAttachment = type.parseAttachment(buff);

        assertEquals(new CCBidOrderPlacementAttachment(ASSET_ID, 100, 2), parsedAttachment);
    }

    @Test
    void parseAttachmentFromJson() throws AplException.NotValidException {
        JSONObject json = new JSONObject();
        json.put("version.BidOrderPlacement", 1L);
        json.put("asset", "1");
        json.put("quantityATU", 100L);
        json.put("priceATM", 5L);

        CCBidOrderPlacementAttachment parsedAttachment = type.parseAttachment(json);

        assertEquals(new CCBidOrderPlacementAttachment(ASSET_ID, 100, 5), parsedAttachment);

    }

    @Test
    void applyAttachmentUnconfirmed_DoubleSpending() {
        mockAttachment(200, 10);
        doReturn(1800L).when(sender).getUnconfirmedBalanceATM();

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertFalse(applied, "BID_ORDER_PLACEMENT should NOT be applied unconfirmed successfully, since the " +
            "sender's account has not enough money 1800 to pay order's total amount 2000");
        verifyNoInteractions(accountService);
    }

    @Test
    void applyAttachmentUnconfirmed_OK() {
        mockAttachment(200, 10);
        doReturn(2100L).when(sender).getUnconfirmedBalanceATM();

        boolean applied = type.applyAttachmentUnconfirmed(tx, sender);

        assertTrue(applied, "BID_ORDER_PLACEMENT should be applied unconfirmed successfully, since the " +
            "sender's account has enough money 2100 to pay order's total amount 2000");
        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.ASSET_BID_ORDER_PLACEMENT, 0, -2000);
    }

    @Test
    void applyAttachment() {
        CCBidOrderPlacementAttachment attachment = mockAttachment(50, 2);

        type.applyAttachment(tx, sender, null);

        verify(orderMatchService).addBidOrder(tx, attachment);
    }

    @Test
    void undoAttachmentUnconfirmed() {
        mockAttachment(100, 2);

        type.undoAttachmentUnconfirmed(tx, sender);

        verify(accountService).addToUnconfirmedBalanceATM(sender, LedgerEvent.ASSET_BID_ORDER_PLACEMENT, 0, 200);
    }

    private CCBidOrderPlacementAttachment mockAttachment(long quantity, long price) {
        CCBidOrderPlacementAttachment attachment = new CCBidOrderPlacementAttachment(ASSET_ID, quantity, price);
        doReturn(attachment).when(tx).getAttachment();
        return attachment;
    }

}