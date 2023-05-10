/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.db;

import com.apollocurrency.aplwallet.apl.util.injectable.DbProperties;

public interface DatabaseAdministratorFactory {

    DatabaseAdministrator createDbAdmin(DbProperties dbProperties);

}
