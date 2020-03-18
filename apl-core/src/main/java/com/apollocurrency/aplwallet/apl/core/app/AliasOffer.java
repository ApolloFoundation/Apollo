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
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author silaev-firstbridge on 3/17/2020
 */
@Slf4j
@Getter
@Setter
@ToString(callSuper = true)
public class AliasOffer extends VersionedDerivedEntity {
    private final long aliasId;
    private long priceATM;
    private long buyerId;

    public AliasOffer(long aliasId, long priceATM, long buyerId, int height) {
        super(aliasId, height);
        this.priceATM = priceATM;
        this.buyerId = buyerId;
        this.aliasId = aliasId;
    }

    public AliasOffer(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.aliasId = rs.getLong("id");
        this.priceATM = rs.getLong("price");
        this.buyerId = rs.getLong("buyer_id");
        setDbKey(dbKey);
    }
}
