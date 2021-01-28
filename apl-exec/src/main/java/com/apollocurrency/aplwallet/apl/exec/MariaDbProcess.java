/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import io.firstbridge.process.db.DbControl;
import io.firstbridge.process.db.DbRunParams;
import io.firstbridge.process.impl.MariaDbRunParamsBuilder;
import io.firstbridge.process.impl.MariaDbControl;
import java.nio.file.Path;
import java.time.Duration;

/**
 * MariaDB/rocksDB is Apollo database. It shouдd be installed from
 * apollo-mariadb package or just be available by configured URL This class
 * tries to start MariaDB from installed package
 *
 * @author Oleksiy Lukin alukin@gmail.com
 */
public class MariaDbProcess {

    private final DbControl dbControl;
    private final DbRunParams dbParams;
    private static Path confFile;
    
    public MariaDbProcess(Path dbDataDir, Path dbInstallDir) {

        confFile = Path.of("my-apl-user.conf");

        dbParams = new MariaDbRunParamsBuilder()
                .dbConfigFile(confFile)
                .dbDataDir(dbDataDir)
                .dbInstallDir(dbInstallDir)
                .redirectErr(Path.of("maria_err.log"))
                .redirectOut(Path.of("maria_out.log"))
                .dbUser("apl")
                .dbPassword("apl")
                .build();
        dbControl = new MariaDbControl(dbParams);
    }
    
    public static MariaDbProcess findRunning(Path dbDataDir, Path dbInstallDir){
        MariaDbProcess process = null;
        DbRunParams params = new MariaDbRunParamsBuilder()
                .dbConfigFile(confFile)
                .dbDataDir(dbDataDir)
                .dbInstallDir(dbInstallDir)
                .redirectErr(Path.of("maria_err.log"))
                .redirectOut(Path.of("maria_out.log"))
                .dbUser("apl")
                .dbPassword("apl")
                .build();
        return process;
    }
    
    public boolean isOK() {
        boolean res = dbControl.isOK();
        return res;
    }

    public boolean start() {
        boolean res = false;

        if (!dbControl.isOK()) {
            res = dbControl.spawnServer();
        }
        return res;
    }

    public boolean stop() {
        boolean res = dbControl.stopServer(Duration.ofSeconds(10));
        return res;
    }
}
