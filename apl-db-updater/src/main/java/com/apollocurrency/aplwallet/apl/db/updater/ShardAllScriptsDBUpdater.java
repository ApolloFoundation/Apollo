package com.apollocurrency.aplwallet.apl.db.updater;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateOutput;
import org.flywaydb.core.api.output.MigrateResult;

@Slf4j
public class ShardAllScriptsDBUpdater implements DBUpdater {

    @Override
    public void update(String url, String user, String password) {
        Flyway flyway =
            Flyway.configure()
                .dataSource(url, user, password)
                .locations("classpath:db/migration/shard")
                .load();

        MigrateResult migrateResult = flyway.migrate();
        log.info("ShardAddConstrainsDBUpdater: flyway version: {},", migrateResult.flywayVersion);
        for (MigrateOutput migration : migrateResult.migrations) {
            log.info("Migration version: {}, path: {}", migration.version, migration.filepath);
        }
    }
}
