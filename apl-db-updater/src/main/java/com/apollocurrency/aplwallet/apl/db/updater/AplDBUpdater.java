package com.apollocurrency.aplwallet.apl.db.updater;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateOutput;
import org.flywaydb.core.api.output.MigrateResult;

@Slf4j
public class AplDBUpdater implements DBUpdater {

    @Override
    public void update(MigrationParams params) {
        Flyway flyway =
            Flyway.configure()
                .dataSource(params.getDbUrl(), params.getUser(), params.getPassword())
                .locations("classpath:db/migration/" + params.getDbType().toLowerCase() + "/apl")
                .load();

        MigrateResult migrateResult = flyway.migrate();

        log.info("AplDBUpdater: flyway version: {},", migrateResult.flywayVersion);
        for (MigrateOutput migration : migrateResult.migrations) {
            log.info("Migration version: {}, path: {}", migration.version, migration.filepath);
        }
    }
}
