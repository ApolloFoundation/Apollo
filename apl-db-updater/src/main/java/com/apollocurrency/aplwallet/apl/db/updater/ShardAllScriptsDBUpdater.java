package com.apollocurrency.aplwallet.apl.db.updater;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateOutput;
import org.flywaydb.core.api.output.MigrateResult;

@Slf4j
public class ShardAllScriptsDBUpdater implements DBUpdater {

    @Override
    public void update(MigrationParams migrationParams) {
        Flyway flyway =
            Flyway.configure()
                .dataSource(migrationParams.getDbUrl(), migrationParams.getUser(), migrationParams.getPassword())
                .locations("classpath:db/migration/" + migrationParams.getDbType() +"/shard")
                .load();

        MigrateResult migrateResult = flyway.migrate();
        log.info("ShardAddConstrainsDBUpdater: flyway version: {},", migrateResult.flywayVersion);
        for (MigrateOutput migration : migrateResult.migrations) {
            log.info("Migration version: {}, path: {}", migration.version, migration.filepath);
        }
    }
}
