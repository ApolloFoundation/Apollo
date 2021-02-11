/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
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

    private DbControl dbControl;
    private static Path confFile;

    public MariaDbProcess(DbConfig conf, Path dbInstallDir, Path dbDataDir) {

        confFile = Path.of("my-apl-user.conf");
        
        DbRunParams dbParams = new MariaDbRunParamsBuilder()
                .dbConfigFile(confFile)
                .dbDataDir(dbDataDir)
                .dbInstallDir(dbInstallDir)
                .redirectOut(Path.of("maria_out.log"))
                .dbUser("apl")
                .dbPassword("apl")
                .build();
        dbControl = new MariaDbControl(dbParams);
    }

    public static MariaDbProcess findRunning(DbConfig conf, Path dbDataDir, Path dbInstallDir){
        MariaDbProcess process = null;
        DbRunParams params = new MariaDbRunParamsBuilder()
                .dbConfigFile(confFile)
                .dbDataDir(dbDataDir)
                .dbInstallDir(dbInstallDir)
                .redirectOut(Path.of("maria_out.log"))
                .dbUser("apl")
                .dbPassword("apl")
                .build();
        MariaDbControl control = new MariaDbControl(params);
        if( control.findRunning()){
            process = new MariaDbProcess(conf,dbDataDir,dbInstallDir);
            process.dbControl = control;
        }
        return process;
    }

    public boolean isOK() {
        boolean res = dbControl.isOK();
        return res;
    }

    public boolean startAndWaitWhenReady() {
        boolean res = false;

        if (!dbControl.isOK()) {
            res = dbControl.spawnServer();
        }
        //May be we have to wait a bit here...
        if(res){
            String script = "SELECT 1;";
            res = dbControl.runQuery(script, "mysql", null);
        }
        return res;
    }

    public boolean stop() {
        boolean res = dbControl.stopServer(Duration.ofSeconds(10));
        return res;
    }
}
