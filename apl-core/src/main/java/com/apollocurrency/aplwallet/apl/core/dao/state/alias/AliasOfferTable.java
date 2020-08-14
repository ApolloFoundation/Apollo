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

package com.apollocurrency.aplwallet.apl.core.dao.state.alias;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.VersionedDeletableEntityDbTable;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.DbKey;
import com.apollocurrency.aplwallet.apl.core.dao.state.keyfactory.LongKeyFactory;
import com.apollocurrency.aplwallet.apl.core.db.DbClause;
import com.apollocurrency.aplwallet.apl.core.db.DbUtils;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.Alias;
import com.apollocurrency.aplwallet.apl.core.entity.state.alias.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.service.appdata.DatabaseManager;
import com.apollocurrency.aplwallet.apl.core.service.state.DerivedTablesRegistry;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author silaev-firstbridge on 3/17/2020
 */
@Singleton
public class AliasOfferTable extends VersionedDeletableEntityDbTable<AliasOffer> {

    private static final LongKeyFactory<AliasOffer> offerDbKeyFactory = new LongKeyFactory<>("id") {

        @Override
        public DbKey newKey(AliasOffer offer) {
            if (offer.getDbKey() == null) {
                offer.setDbKey(super.newKey(offer.getAliasId()));
            }
            return offer.getDbKey();
        }
    };

    @Inject
    public AliasOfferTable(DerivedTablesRegistry derivedDbTablesRegistry,
                           DatabaseManager databaseManager) {
        super("alias_offer", offerDbKeyFactory, null,
            derivedDbTablesRegistry, databaseManager, null);
    }

    @Override
    public AliasOffer load(Connection con, ResultSet rs, DbKey dbKey) throws SQLException {
        return new AliasOffer(rs, dbKey);
    }

    @Override
    public void save(Connection con, AliasOffer offer) throws SQLException {
        try (
            @DatabaseSpecificDml(DmlMarker.MERGE)
            PreparedStatement pstmt = con.prepareStatement("INSERT INTO alias_offer (id, price, buyer_id, height) "
                + "VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "id = VALUES(id), price = VALUES(price), buyer_id = VALUES(buyer_id), height = VALUES(height)")
        ) {

            int i = 0;
            pstmt.setLong(++i, offer.getAliasId());
            pstmt.setLong(++i, offer.getPriceATM());
            DbUtils.setLongZeroToNull(pstmt, ++i, offer.getBuyerId());
            pstmt.setInt(++i, offer.getHeight());
            pstmt.executeUpdate();
        }
    }

    public AliasOffer getOffer(Alias alias) {
        return getBy(new DbClause.LongClause("id", alias.getId()).and(new DbClause.LongClause("price", DbClause.Op.NE, Long.MAX_VALUE)));
    }
}
