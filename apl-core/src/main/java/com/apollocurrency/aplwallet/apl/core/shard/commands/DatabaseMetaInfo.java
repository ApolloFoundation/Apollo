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

package com.apollocurrency.aplwallet.apl.core.shard.commands;

import com.apollocurrency.aplwallet.apl.core.app.Block;
import com.apollocurrency.aplwallet.apl.core.db.TransactionalDataSource;
import com.apollocurrency.aplwallet.apl.core.shard.MigrateState;

/**
 * Interface + Implementation for creating SOURCE and TARGET database instance information
 *
 * @author yuriy.larin
 */
public interface DatabaseMetaInfo {

    TransactionalDataSource getDataSource();

    void setDataSource(TransactionalDataSource dataSource);

    String getNewFileName();

    void setNewFileName(String newFileName);

/*
    List<String> getStatementList();

    void setStatementList(List<String> statementList);
*/

    int getCommitBatchSize();

    void setCommitBatchSize(int commitBatchSize);

    MigrateState getMigrateState();

    void setMigrateState(MigrateState migrateState);

    Block getSnapshotBlock();

    void setSnapshotBlock(Block snapshotBlock);

    Long getSnapshotBlockHeight();

    void setSnapshotBlockHeight(Long snapshotBlockHeight);

}
