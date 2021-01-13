/*
 * Copyright © 2019-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.appdata;

import java.util.List;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.ActiveGenerator;

public interface ActiveGeneratorService {
    List<ActiveGenerator> getNextGenerators();
}
