/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.db.updater.DBUpdater;

public interface DatabaseAdministrator {

    void startDatabase();

    void deleteDatabase();

    String createDatabase();

    void migrateDatabase(DBUpdater dbUpdater);

    void stopDatabase();
}
