package com.apollocurrency.aplwallet.apl.db.updater;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateOutput;
import org.flywaydb.core.api.output.MigrateResult;

@Slf4j
public class ShardInitDBUpdater implements DBUpdater {
    @Override
    public void update(String url, String user, String password) {
        Flyway flyway =
            Flyway.configure()
                .dataSource(url, user, password)
                .locations("filesystem:apl-db-updater/src/main/resources/db/migration/shard")
                .target(MigrationVersion.fromVersion("1.0"))
                .load();

        MigrateResult migrateResult = flyway.migrate();

        log.info("ShardInitDBUpdater: flyway version: {},", migrateResult.flywayVersion);
        for (MigrateOutput migration : migrateResult.migrations) {
            log.info("Migration version: {}, path: {}", migration.version, migration.filepath);
        }
    }
}
