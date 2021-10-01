/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.config;

import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.BigIntegerArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.DexCurrenciesFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.IntArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.LongArrayArgumentFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.OrderStatusFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.OrderTypeFactory;
import com.apollocurrency.aplwallet.apl.core.dao.appdata.factory.ShardStateFactory;
import com.apollocurrency.aplwallet.apl.core.db.DatabaseManager;
import com.apollocurrency.aplwallet.apl.util.annotation.DatabaseSpecificDml;
import com.apollocurrency.aplwallet.apl.util.annotation.DmlMarker;
import com.apollocurrency.aplwallet.apl.util.cdi.transaction.JdbiHandleFactory;
import lombok.extern.slf4j.Slf4j;
import org.jdbi.v3.core.ConnectionException;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Optional;

@Singleton
@Slf4j
public class JdbiConfiguration {

    private final DatabaseManager dbManager;
    private volatile Jdbi jdbi;

    @Inject
    public JdbiConfiguration(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @PostConstruct
    public void init() {
        log.info("Attempting to create Jdbi instance...");
        jdbi = Jdbi.create(dbManager.getDataSource().original());
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.registerArgument(new BigIntegerArgumentFactory());
        jdbi.registerArgument(new DexCurrenciesFactory());
        jdbi.registerArgument(new OrderTypeFactory());
        jdbi.registerArgument(new OrderStatusFactory());
        jdbi.registerArgument(new LongArrayArgumentFactory());
        jdbi.registerArgument(new IntArrayArgumentFactory());
        jdbi.registerArrayType(long.class, "generatorIds");
        jdbi.registerArgument(new ShardStateFactory());

        log.info("Attempting to open Jdbi handler to database..");
        try (Handle handle = jdbi.open()) {
            @DatabaseSpecificDml(DmlMarker.DUAL_TABLE_USE)
            Optional<Integer> result = handle.createQuery("select 1 from dual;")
                .mapTo(Integer.class).findOne();
            log.debug("check SQL result ? = {}", result);
        } catch (ConnectionException e) {
            log.error("Error on opening database connection", e);
            throw e;
        }
        log.info("Jdbi initialization is done for the datasource {}", dbManager.getDataSource());
    }

    @Produces
    @Singleton
    public Jdbi jdbi() {
        return jdbi;
    }

    @Produces
    @Singleton
    public JdbiHandleFactory factory() {
        return new JdbiHandleFactory(jdbi);
    }
}
