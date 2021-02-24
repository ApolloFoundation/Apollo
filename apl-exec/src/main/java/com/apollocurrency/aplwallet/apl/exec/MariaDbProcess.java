/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import com.apollocurrency.aplwallet.apl.core.db.DbConfig;
import io.firstbridge.process.db.DbControl;
import io.firstbridge.process.impl.MariaDbControl;
import io.firstbridge.process.impl.MariaDbRunParams;


import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * MariaDB/rocksDB is Apollo database. It shouдd be installed from
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
    
    private MariaDbRunParams setDbParams(DbConfig conf, Path dbInstallDir, Path dbDataDir){
        dbParams = new MariaDbRunParams();
        confFile = dbDataDir.resolve(DB_CONF_FILE);
        confFileTemplate = dbInstallDir.resolve("conf").resolve(DB_CONF_FILE_TEMPLATE);
        String dbUser = conf.getDbConfig().getDbUsername();
        String dbPassword = conf.getDbConfig().getDbPassword();
        
        Map<String,String> vars = new HashMap<>();
        vars.put("apl_db_dir", dbDataDir.toAbsolutePath().toString());
        vars.put("apl_mariadb_pkg_dir", dbInstallDir.toAbsolutePath().toString());
        
                dbParams.setDbConfigFileTemplate(confFileTemplate);
                dbParams.setVarSubstMap(vars);
                dbParams.setDbConfigFile(confFile);
                dbParams.setDbDataDir(dbDataDir);
                dbParams.setDbInstallDir(dbInstallDir);
                dbParams.setOut(Path.of("maria_out.log"));
                dbParams.setDbUser(dbUser);
                dbParams.setDbPassword(dbPassword);    
        
                if(dbParams.verify()){
                    dbParams.processConfigTemplates();
                }else{
                   log.error("Please verify database parameters!");
                }
                
                
        return dbParams;
    }
    
    public MariaDbProcess(DbConfig conf, Path dbInstallDir, Path dbDataDir) {
                
        dbControl = new MariaDbControl(setDbParams(conf, dbInstallDir, dbDataDir));        
    }

    public static MariaDbProcess findRunning(DbConfig conf, Path dbDataDir, Path dbInstallDir){
        MariaDbProcess process = new MariaDbProcess(conf, dbInstallDir, dbDataDir);
        process.dbControl = new MariaDbControl(process.dbParams);
        if( process.dbControl.findRunning()){
            return process;
        }
        return null;
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
            res = dbControl.runQuery(script, "mysql", null, dbParams.getDbUser());
        }
        return res;
    }

    public boolean stop() {
        boolean res = dbControl.stopServer(Duration.ofSeconds(10));
        return res;
    }
}
