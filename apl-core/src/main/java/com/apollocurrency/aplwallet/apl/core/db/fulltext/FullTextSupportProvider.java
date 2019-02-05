/*
 *  Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.db.fulltext;

import java.util.concurrent.ConcurrentHashMap;

public class FullTextSupportProvider {
    /** Index triggers */
    private static final ConcurrentHashMap<String, FullTextTrigger> indexTriggers = new ConcurrentHashMap<>();
}
