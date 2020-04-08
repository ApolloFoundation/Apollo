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

package com.apollocurrency.aplwallet.apl.core.alias.service;

import com.apollocurrency.aplwallet.apl.core.alias.converter.IteratorToStreamConverter;
import com.apollocurrency.aplwallet.apl.core.alias.dao.AliasOfferTable;
import com.apollocurrency.aplwallet.apl.core.alias.dao.AliasTable;
import com.apollocurrency.aplwallet.apl.core.alias.entity.Alias;
import com.apollocurrency.aplwallet.apl.core.alias.entity.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.stream.Stream;

@Singleton
public class AliasServiceImpl implements AliasService {
    private final AliasTable aliasTable;
    private final AliasOfferTable offerTable;
    private final Blockchain blockchain;
    private final IteratorToStreamConverter<Alias> converter;

    /**
     * Constrictor for unit tests.
     *
     * @param aliasTable
     * @param offerTable
     * @param blockchain
     * @param converter
     */
    public AliasServiceImpl(
        final AliasTable aliasTable,
        final AliasOfferTable offerTable,
        final Blockchain blockchain,
        final IteratorToStreamConverter<Alias> converter
    ) {
        this.aliasTable = aliasTable;
        this.offerTable = offerTable;
        this.blockchain = blockchain;
        this.converter = converter;
    }

    @Inject
    public AliasServiceImpl(
        final AliasTable aliasTable,
        final AliasOfferTable offerTable,
        final Blockchain blockchain
    ) {
        this.aliasTable = aliasTable;
        this.offerTable = offerTable;
        this.blockchain = blockchain;
        this.converter = new IteratorToStreamConverter<>();
    }

    @Override
    public int getCount() {
        return aliasTable.getCount();
    }

    @Override
    public int getAccountAliasCount(long accountId) {
        return aliasTable.getCount(new DbClause.LongClause("account_id", accountId));
    }

    @Override
    public Stream<Alias> getAliasesByOwner(long accountId, int from, int to) {
        final DbIterator<Alias> aliasIterator = aliasTable.getManyBy(
            new DbClause.LongClause("account_id", accountId), from, to
        );

        return converter.convert(aliasIterator);
    }

    @Override
    public Alias getAliasByName(String aliasName) {
        verifyAliasName(aliasName);

        return aliasTable.getBy(new DbClause.StringClause("alias_name_lower", aliasName.toLowerCase()));
    }

    @Override
    public Stream<Alias> getAliasesByNamePattern(String aliasName, int from, int to) {
        verifyAliasName(aliasName);

        final DbIterator<Alias> aliasIterator = aliasTable.getManyBy(
            new DbClause.LikeClause("alias_name_lower", aliasName.toLowerCase()), from, to
        );

        return converter.convert(aliasIterator);
    }

    @Override
    public Alias getAliasById(long id) {
        return aliasTable.getAlias(id);
    }

    @Override
    public void deleteAlias(final String aliasName) {
        verifyAliasName(aliasName);

        final Alias alias = getAliasByName(aliasName);
        final AliasOffer offer = offerTable.getOffer(alias);
        final int height = blockchain.getHeight();
        if (offer != null) {
            offer.setPriceATM(Long.MAX_VALUE);
            offer.setHeight(height);
            offerTable.deleteAtHeight(offer, height);
        }
        alias.setHeight(height);
        aliasTable.deleteAtHeight(alias, height);
    }

    @Override
    public void addOrUpdateAlias(
        final Transaction transaction,
        final MessagingAliasAssignment attachment
    ) {
        verifyTransaction(transaction);
        Objects.requireNonNull(attachment, "attachment is not supposed to be null");

        Alias alias = getAliasByName(attachment.getAliasName());
        if (alias == null) {
            alias = new Alias(transaction, attachment, blockchain.getHeight(), blockchain.getLastBlockTimestamp());
        } else {
            alias.setHeight(blockchain.getHeight());
            alias.setAccountId(transaction.getSenderId());
            alias.setAliasURI(attachment.getAliasURI());
            alias.setTimestamp(blockchain.getLastBlockTimestamp());
        }
        aliasTable.insert(alias);
    }

    @Override
    public void sellAlias(
        final Transaction transaction,
        final MessagingAliasSell attachment
    ) {
        verifyTransaction(transaction);
        Objects.requireNonNull(attachment, "attachment is not supposed to be null");

        final String aliasName = attachment.getAliasName();
        final long priceATM = attachment.getPriceATM();
        final long buyerId = transaction.getRecipientId();
        if (priceATM > 0) {
            Alias alias = getAliasByName(aliasName);
            AliasOffer offer = offerTable.getOffer(alias);
            if (offer == null) {
                offerTable.insert(new AliasOffer(alias.getId(), priceATM, buyerId, blockchain.getHeight()));
            } else {
                offer.setHeight(blockchain.getHeight());
                offer.setPriceATM(priceATM);
                offer.setBuyerId(buyerId);
                offerTable.insert(offer);
            }
        } else {
            changeOwner(buyerId, aliasName);
        }
    }

    @Override
    public void changeOwner(long newOwnerId, String aliasName) {
        verifyAliasName(aliasName);

        Alias alias = getAliasByName(aliasName);
        alias.setHeight(blockchain.getHeight());
        alias.setAccountId(newOwnerId);
        alias.setTimestamp(blockchain.getLastBlockTimestamp());
        aliasTable.insert(alias);
        AliasOffer offer = offerTable.getOffer(alias);
        if (offer != null) {
            offer.setHeight(blockchain.getHeight());
            offer.setPriceATM(Long.MAX_VALUE);
            offerTable.deleteAtHeight(offer, blockchain.getHeight());
        }
    }

    @Override
    public AliasOffer getOffer(Alias alias) {
        Objects.requireNonNull(alias, "alias is not supposed to be null");

        return offerTable.getBy(new DbClause.LongClause("id", alias.getId()).and(new DbClause.LongClause("price", DbClause.Op.NE, Long.MAX_VALUE)));
    }

    private void verifyTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "transaction is not supposed to be null");
    }

    private void verifyAliasName(String aliasName) {
        Objects.requireNonNull(aliasName, "aliasName is not supposed to be null");
    }
}
