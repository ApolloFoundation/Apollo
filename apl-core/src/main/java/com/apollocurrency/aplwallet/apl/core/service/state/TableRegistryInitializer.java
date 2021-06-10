/*
 * Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.service.state;

import com.apollocurrency.aplwallet.apl.core.dao.state.derived.DerivedTableInterface;
import com.apollocurrency.aplwallet.apl.core.dao.state.derived.SearchableTableInterface;
import com.apollocurrency.aplwallet.apl.core.service.fulltext.FullTextConfig;
import com.apollocurrency.aplwallet.apl.util.cdi.config.Property;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class TableRegistryInitializer {
    @Inject
    Instance<DerivedTableInterface<?>> tables;
    @Inject
    DerivedTablesRegistry registry;
    @Inject
    FullTextConfig fullTextConfig;
    @Inject
    @Property(value = "apl.derivedTablesCount", defaultValue = "55")
    int requiredTablesCount;

    @PostConstruct
    public void registerAllTables() {
        long count = tables.stream().count();
        if (count != requiredTablesCount) {
            throw new IllegalStateException("Should be registered 55 derived tables, got " + count + ", list: " +
                tables.stream().map(Objects::toString).collect(Collectors.joining(",")));
        }
        tables.forEach(this::init);
        log.info("Registered {} derived tables", count);
    }

    private void init(DerivedTableInterface<?> table) {
        registry.registerDerivedTable(table);
        log.debug("Register derived class: {}", table.getClass().getName());
        if (table instanceof SearchableTableInterface) {
            log.debug("Register SearchableTable derived class: {}", table.getClass().getName());
            fullTextConfig.registerTable(table.getName(), table.getFullTextSearchColumns());
        }
    }
}