/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.alias.AliasOfferTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.alias.AliasTable;
import com.apollocurrency.aplwallet.apl.util.db.DbClause;
import com.apollocurrency.aplwallet.apl.util.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.model.Transaction;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.Blockchain;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author silaev-firstbridge on 3/19/2020
 */
@ExtendWith(MockitoExtension.class)
class AliasServiceTest {
    @Mock
    private AliasTable aliasTable;
    @Mock
    private AliasOfferTable offerTable;
    @Mock
    private Blockchain blockchain;
    private AliasServiceImpl aliasService;

    @BeforeEach
    void setUp() {
        this.aliasService = spy(new AliasServiceImpl(
            aliasTable,
            offerTable,
            blockchain
        ));
    }

    @Test
    void shouldGetCount() {
        //GIVEN

        //WHEN
        aliasService.getCount();

        //THEN
        verify(aliasTable, times(1)).getCount();
    }

    @Test
    void shouldGetAccountAliasCount() {
        //GIVEN
        final long accountId = 7821792282123976600L;

        //WHEN
        aliasService.getAccountAliasCount(accountId);

        //THEN
        verify(aliasTable, times(1)).getCount(any(DbClause.LongClause.class));
    }

    @Test
    void shouldGetAliasesByOwner() {
        //GIVEN
        final long accountId = 7821792282123976600L;
        final int from = 1;
        final int to = 2;
        doReturn(mock(DbIterator.class)).when(aliasTable).getManyBy(any(DbClause.class), anyInt(), anyInt());

        //WHEN

        List<Alias> aliases = aliasService.getAliasesByOwner(accountId, 0, from, to);

        //THEN
        assertEquals(List.of(), aliases);
    }

    @Test
    void shouldGetAliasByName() {
        //GIVEN
        final String aliasName = "AS1561618989348";

        //WHEN
        aliasService.getAliasByName(aliasName);

        //THEN
        verify(aliasTable, times(1)).getBy(any(DbClause.StringClause.class));
    }

    @Test
    void shouldGetAliasesByNamePattern() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final int from = 1;
        final int to = 2;
        doReturn(mock(DbIterator.class)).when(aliasTable).getManyBy(any(DbClause.class), anyInt(), anyInt());

        //WHEN
        List<Alias> aliases = aliasService.getAliasesByNamePattern(aliasName, from, to);

        //THEN
        verify(aliasTable, times(1))
            .getManyBy(any(DbClause.LikeClause.class), eq(from), eq(to));
        assertEquals(List.of(), aliases);
    }

    @Test
    void shouldGetAliasById() {
        //GIVEN
        final long id = 1815781403495190811L;

        //WHEN
        aliasService.getAliasById(id);

        //THEN
        verify(aliasTable, times(1)).getAlias(id);
    }

    @Test
    void shouldDeleteOnlyAlias() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final Alias alias = mock(Alias.class);
        final int height = 1445;
        when(aliasTable.getBy(any(DbClause.StringClause.class))).thenReturn(alias);
        when(blockchain.getHeight()).thenReturn(height);

        //WHEN
        aliasService.deleteAlias(aliasName);

        //THEN
        verify(offerTable, times(0)).deleteAtHeight(null, height);
        verify(aliasTable, times(1)).deleteAtHeight(alias, height);
    }

    @Test
    void shouldDeleteAliasAndOffer() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final Alias alias = mock(Alias.class);
        final int height = 1445;
        final AliasOffer offer = mock(AliasOffer.class);
        when(aliasTable.getBy(any(DbClause.StringClause.class))).thenReturn(alias);
        when(blockchain.getHeight()).thenReturn(height);
        when(offerTable.getOffer(alias)).thenReturn(offer);

        //WHEN
        aliasService.deleteAlias(aliasName);

        //THEN
        verify(offerTable, times(1)).deleteAtHeight(offer, height);
        verify(aliasTable, times(1)).deleteAtHeight(alias, height);
    }

    @Test
    void shouldAddNewAlias() {
        //GIVEN
        final Transaction transaction = mock(Transaction.class);
        final MessagingAliasAssignment attachment = mock(MessagingAliasAssignment.class);
        final int height = 1445;
        final int timestamp = 45687775;
        final String aliasName = "AS1561618989348";
        when(attachment.getAliasName()).thenReturn(aliasName);
        when(blockchain.getHeight()).thenReturn(height);
        when(blockchain.getLastBlockTimestamp()).thenReturn(timestamp);
        final Alias aliasExpected = new Alias(transaction, attachment, height, timestamp);

        //WHEN
        aliasService.addOrUpdateAlias(transaction, attachment);

        //THEN
        verify(aliasTable, times(1)).insert(aliasExpected);
    }

    @Test
    void shouldUpdateAlias() {
        //GIVEN
        final Transaction transaction = mock(Transaction.class);
        final MessagingAliasAssignment attachment = mock(MessagingAliasAssignment.class);
        final int height = 1445;
        final int timestamp = 45687775;
        final String aliasName = "AS1561618989348";
        final long senderId = 123L;
        final String aliasURI = "aliasURI";
        final Alias alias = mock(Alias.class);
        when(attachment.getAliasName()).thenReturn(aliasName);
        when(aliasTable.getBy(any(DbClause.StringClause.class))).thenReturn(alias);
        when(blockchain.getHeight()).thenReturn(height);
        when(blockchain.getLastBlockTimestamp()).thenReturn(timestamp);
        when(transaction.getSenderId()).thenReturn(senderId);
        when(attachment.getAliasURI()).thenReturn(aliasURI);

        //WHEN
        aliasService.addOrUpdateAlias(transaction, attachment);

        //THEN
        verify(aliasTable, times(1)).insert(alias);
        verify(alias, times(1)).setHeight(height);
        verify(alias, times(1)).setAccountId(senderId);
        verify(alias, times(1)).setAliasURI(aliasURI);
        verify(alias, times(1)).setTimestamp(timestamp);
    }

    @Test
    void shouldSellAliasHavingNoOfferAndPriceATM() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final long priceATM = 10000L;
        final Alias alias = mock(Alias.class);
        final Transaction transaction = mock(Transaction.class);
        final MessagingAliasSell attachment = mock(MessagingAliasSell.class);
        final int height = 1445;
        final long buyerId = 55;
        when(attachment.getAliasName()).thenReturn(aliasName);
        when(attachment.getPriceATM()).thenReturn(priceATM);
        when(aliasTable.getBy(any(DbClause.StringClause.class))).thenReturn(alias);
        when(transaction.getRecipientId()).thenReturn(buyerId);
        when(attachment.getAliasName()).thenReturn(aliasName);
        when(blockchain.getHeight()).thenReturn(height);
        final AliasOffer offerExpected =
            new AliasOffer(alias.getId(), priceATM, buyerId, blockchain.getHeight());

        //WHEN
        aliasService.sellAlias(transaction, attachment);

        //THEN
        verify(offerTable, times(1)).insert(offerExpected);
    }

    @Test
    void shouldSellAliasHavingOfferAndPriceATM() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final long priceATM = 10000L;
        final Alias alias = mock(Alias.class);
        final Transaction transaction = mock(Transaction.class);
        final MessagingAliasSell attachment = mock(MessagingAliasSell.class);
        final int height = 1445;
        final long buyerId = 55;
        final AliasOffer offer = mock(AliasOffer.class);
        when(attachment.getAliasName()).thenReturn(aliasName);
        when(attachment.getPriceATM()).thenReturn(priceATM);
        when(aliasTable.getBy(any(DbClause.StringClause.class))).thenReturn(alias);
        when(transaction.getRecipientId()).thenReturn(buyerId);
        when(attachment.getAliasName()).thenReturn(aliasName);
        when(blockchain.getHeight()).thenReturn(height);
        when(offerTable.getOffer(alias)).thenReturn(offer);

        //WHEN
        aliasService.sellAlias(transaction, attachment);

        //THEN
        verify(offerTable, times(1)).insert(offer);
        verify(offer, times(1)).setHeight(height);
        verify(offer, times(1)).setPriceATM(priceATM);
        verify(offer, times(1)).setBuyerId(buyerId);
    }

    @Test
    void shouldSellAliasHavingNoPriceATM() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final long priceATM = 0L;
        final Transaction transaction = mock(Transaction.class);
        final MessagingAliasSell attachment = mock(MessagingAliasSell.class);
        final long buyerId = 55;
        when(attachment.getPriceATM()).thenReturn(priceATM);
        when(transaction.getRecipientId()).thenReturn(buyerId);
        doNothing().when(aliasService).changeOwner(buyerId, aliasName);
        when(attachment.getAliasName()).thenReturn(aliasName);

        //WHEN
        aliasService.sellAlias(transaction, attachment);

        //THEN
        verify(aliasService, times(1)).changeOwner(buyerId, aliasName);
    }

    @Test
    void shouldChangeOwnerHavingOffer() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final long newOwnerId = 88;
        final int height = 1445;
        final int timestamp = 45687775;
        final Alias alias = mock(Alias.class);
        when(aliasTable.getBy(any(DbClause.StringClause.class))).thenReturn(alias);
        when(blockchain.getHeight()).thenReturn(height);
        when(blockchain.getLastBlockTimestamp()).thenReturn(timestamp);
        final AliasOffer offer = mock(AliasOffer.class);
        when(offerTable.getOffer(alias)).thenReturn(offer);

        //WHEN
        aliasService.changeOwner(newOwnerId, aliasName);

        //THEN
        verify(aliasTable, times(1)).insert(alias);
        verify(alias, times(1)).setHeight(height);
        verify(alias, times(1)).setAccountId(newOwnerId);
        verify(alias, times(1)).setTimestamp(timestamp);

        verify(offerTable, times(1)).deleteAtHeight(offer, height);
        offer.setHeight(height);
        offer.setPriceATM(Long.MAX_VALUE);
    }

    @Test
    void shouldChangeOwnerHavingNoOffer() {
        //GIVEN
        final String aliasName = "AS1561618989348";
        final long newOwnerId = 88;
        final int height = 1445;
        final int timestamp = 45687775;
        final Alias alias = mock(Alias.class);
        when(aliasTable.getBy(any(DbClause.StringClause.class))).thenReturn(alias);
        when(blockchain.getHeight()).thenReturn(height);
        when(blockchain.getLastBlockTimestamp()).thenReturn(timestamp);

        //WHEN
        aliasService.changeOwner(newOwnerId, aliasName);

        //THEN
        verify(aliasTable, times(1)).insert(alias);
        verify(alias, times(1)).setHeight(height);
        verify(alias, times(1)).setAccountId(newOwnerId);
        verify(alias, times(1)).setTimestamp(timestamp);

        verify(offerTable, times(0)).deleteAtHeight(any(AliasOffer.class), eq(height));
    }

    @Test
    void shouldGetOffer() {
        //GIVEN
        final Alias alias = mock(Alias.class);

        //WHEN
        aliasService.getOffer(alias);

        //THEN
        verify(offerTable, times(1)).getBy(any(DbClause.class));
    }
}