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

package com.apollocurrency.aplwallet.apl.core.shard.helper;

/**
 * Internal wrapper class is used for paginating on tables by select sql statement.
 *
 * @author yuriy.larin
 */
class PaginateResultWrapper {

    public Long limitValue = -1L; // previous column value will be used for next select query
    public Boolean isFinished = Boolean.FALSE; // can be used as sign of ended result

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PaginateResultWrapper{");
        sb.append("limitValue=").append(limitValue);
        sb.append(", isFinished=").append(isFinished);
        sb.append('}');
        return sb.toString();
    }
}
