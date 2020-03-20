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

package com.apollocurrency.aplwallet.apl.core.account.service;

import com.apollocurrency.aplwallet.apl.core.account.dao.AliasOfferTable;
import com.apollocurrency.aplwallet.apl.core.account.dao.AliasTable;
import com.apollocurrency.aplwallet.apl.core.app.Alias;
import com.apollocurrency.aplwallet.apl.core.app.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbIterator;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;
import com.apollocurrency.aplwallet.apl.core.utils.StreamUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Stream;

@Singleton
public class AliasServiceImpl implements AliasService {
    private final AliasTable aliasTable;
    private final AliasOfferTable offerTable;
    private final Blockchain blockchain;

    @Inject
    public AliasServiceImpl(
        final AliasTable aliasTable,
        final AliasOfferTable offerTable,
        final Blockchain blockchain
    ) {
        this.aliasTable = aliasTable;
        this.offerTable = offerTable;
        this.blockchain = blockchain;
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

        return StreamUtils.getStreamFromIterator(aliasIterator);
    }

    @Override
    public Alias getAlias(String aliasName) {
        return aliasTable.getBy(new DbClause.StringClause("alias_name_lower", aliasName.toLowerCase()));
    }

    @Override
    public Stream<Alias> getAliasesLike(String aliasName, int from, int to) {
        final DbIterator<Alias> aliasIterator = aliasTable.getManyBy(
            new DbClause.LikeClause("alias_name_lower", aliasName.toLowerCase()), from, to
        );

        return StreamUtils.getStreamFromIterator(aliasIterator);
    }

    @Override
    public Alias getAlias(long id) {
        return aliasTable.getAlias(id);
    }

    @Override
    public void deleteAlias(final String aliasName) {
        final Alias alias = getAlias(aliasName);
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
    public void addOrUpdateAlias(Transaction transaction, MessagingAliasAssignment attachment) {
        Alias alias = getAlias(attachment.getAliasName());
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
    public void sellAlias(Transaction transaction, MessagingAliasSell attachment) {
        final String aliasName = attachment.getAliasName();
        final long priceATM = attachment.getPriceATM();
        final long buyerId = transaction.getRecipientId();
        if (priceATM > 0) {
            Alias alias = getAlias(aliasName);
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
        Alias alias = getAlias(aliasName);
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
        return offerTable.getBy(new DbClause.LongClause("id", alias.getId()).and(new DbClause.LongClause("price", DbClause.Op.NE, Long.MAX_VALUE)));
    }
}
