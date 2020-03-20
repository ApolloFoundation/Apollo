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

package com.apollocurrency.aplwallet.apl.alias.entity;

import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.db.DbKey;
import com.apollocurrency.aplwallet.apl.core.db.model.VersionedDerivedEntity;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Entity class for Alias.
 * <p>
 * Gets EqualsAndHashCode from DerivedEntity.
 */
@Getter
@Setter
@ToString(callSuper = true)
public final class Alias extends VersionedDerivedEntity {
    private final long id;
    private final String aliasName;
    private long accountId;
    private String aliasURI;
    private int timestamp;

    public Alias(Transaction transaction, MessagingAliasAssignment attachment, int height, int timestamp) {
        super(transaction.getId(), height);
        this.id = transaction.getId();
        this.accountId = transaction.getSenderId();
        this.aliasName = attachment.getAliasName();
        this.aliasURI = attachment.getAliasURI();
        this.timestamp = timestamp;
    }

    public Alias(ResultSet rs, DbKey dbKey) throws SQLException {
        super(rs);
        this.id = rs.getLong("id");
        this.accountId = rs.getLong("account_id");
        this.aliasName = rs.getString("alias_name");
        this.aliasURI = rs.getString("alias_uri");
        this.timestamp = rs.getInt("timestamp");
        setDbKey(dbKey);
    }
}
