/*
 * Copyright © 2018-2020 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author silaev-firstbridge
 */
@Singleton
@Slf4j
public class TableRegistryInitializer {
    @Inject
    Instance<DerivedTableInterface<?>> tables;
    @Inject
    DerivedTablesRegistry registry;
    @Inject
    FullTextConfig fullTextConfig;
    @PostConstruct
    public void registerAllTables() {
        tables.forEach(this::init);
        log.info("Registered {} derived tables", tables.stream().count());
    }

    private void init(DerivedTableInterface table) {
        registry.registerDerivedTable(table);
        log.debug("Register derived class: {}", this.getClass().getName());
        if (this instanceof SearchableTableInterface) {
            log.debug("Register SearchableTable derived class: {}", this.getClass().getName());
            fullTextConfig.registerTable(table.getName(), table.getFullTextSearchColumns());
        }
    }
}