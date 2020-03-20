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

package com.apollocurrency.aplwallet.apl.alias.service;

import com.apollocurrency.aplwallet.apl.alias.entity.Alias;
import com.apollocurrency.aplwallet.apl.alias.entity.AliasOffer;
import com.apollocurrency.aplwallet.apl.core.app.Transaction;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasAssignment;
import com.apollocurrency.aplwallet.apl.core.transaction.messages.MessagingAliasSell;

import java.util.stream.Stream;

public interface AliasService {
    int getCount();

    int getAccountAliasCount(long accountId);

    Stream<Alias> getAliasesByOwner(long accountId, int from, int to);

    Alias getAliasByName(String aliasName);

    Stream<Alias> getAliasesByNamePattern(String aliasName, int from, int to);

    Alias getAliasById(long id);

    void deleteAlias(final String aliasName);

    void addOrUpdateAlias(Transaction transaction, MessagingAliasAssignment attachment);

    void sellAlias(Transaction transaction, MessagingAliasSell attachment);

    void changeOwner(long newOwnerId, String aliasName);

    AliasOffer getOffer(Alias alias);
}
