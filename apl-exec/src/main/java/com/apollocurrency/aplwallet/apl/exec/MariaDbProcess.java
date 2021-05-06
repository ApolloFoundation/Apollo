/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import io.firstbridge.process.db.DbControl;
import io.firstbridge.process.impl.MariaDbControl;
import io.firstbridge.process.impl.MariaDbRunParams;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * MariaDB/rocksDB is Apollo database. It should be installed from
 * apollo-mariadb package or just be available by configured URL This class
 * tries to start MariaDB from installed package
 *
 * @author Oleksiy Lukin alukin@gmail.com
 */
@Slf4j
public class MariaDbProcess {
    public static final String DB_CONF_FILE="my-apl.cnf";
    public static final String DB_CONF_FILE_TEMPLATE="my-apl.cnf.template";

    private DbControl dbControl;
    private  Path confFile;
    private  Path confFileTemplate;
    private MariaDbRunParams dbParams;

    private MariaDbRunParams setDbParams(DbConfig conf, Path dbInstallDir, Path dbDataDir, Path logPath){
        dbParams = new MariaDbRunParams();
        confFile = dbDataDir.resolve(DB_CONF_FILE);
        confFileTemplate = dbInstallDir.resolve("conf").resolve(DB_CONF_FILE_TEMPLATE);
        String dbUser = conf.getDbConfig().getDbUsername();
        String dbPassword = conf.getDbConfig().getDbPassword();

        Map<String,String> vars = new HashMap<>();
        vars.put("apl_db_dir", dbDataDir.toAbsolutePath().toString());
        vars.put("apl_mariadb_pkg_dir", dbInstallDir.toAbsolutePath().toString());
        vars.put("dbuser", dbUser);
        vars.put("dbuser_password", dbPassword);

                dbParams.setDbConfigFileTemplate(confFileTemplate);
                dbParams.setVarSubstMap(vars);
                dbParams.setDbConfigFile(confFile);
                dbParams.setDbDataDir(dbDataDir);
                dbParams.setDbInstallDir(dbInstallDir);
                dbParams.setOut(logPath);
                dbParams.setDbUser(dbUser);
                dbParams.setDbPassword(dbPassword);

                if(dbParams.verify()){
                    if(!dbParams.processConfigTemplates()){
                        log.warn("Error processing DB config templates!");
                    }
                }else{
                   log.error("Please verify database parameters!");
                }


        return dbParams;
    }

    public MariaDbProcess(DbConfig conf, Path dbInstallDir, Path dbDataDir, Path logPath) {

        dbControl = new MariaDbControl(setDbParams(conf, dbInstallDir, dbDataDir, logPath));
    }
    /**
    *
    * @param conf set of configuration properties
    * @param dbDataDir Data directory for database
    * @param dbInstallDir directory where MariaDB dirstibution is installed
    * @return MariaDBProcess instance that should be checked with isOK() method
    */
    public static MariaDbProcess findRunning(DbConfig conf, Path dbDataDir, Path dbInstallDir, Path logPath){
        MariaDbProcess process = new MariaDbProcess(conf, dbInstallDir, dbDataDir, logPath);
        process.dbControl = new MariaDbControl(process.dbParams);
        process.dbControl.findRunning();
        return process;
    }

    public boolean isOK() {
        boolean res = (dbControl!=null && dbControl.isOK());
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
            res = dbControl.runQuery(script, "mysql", null, dbParams.getDbUser(), dbParams.getDbPassword());
        }
        return res;
    }

    public boolean stop() {
        boolean res = dbControl.stopServer(Duration.ofSeconds(10));
        return res;
    }
}
