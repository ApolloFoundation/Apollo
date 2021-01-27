/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.exec;

import io.firstbridge.process.db.DbControl;
import io.firstbridge.process.db.DbRunParams;
import io.firstbridge.process.impl.MariaDbRunParamsBuilder;
import io.firstbridge.process.impl.MariaDbControl;
import java.nio.file.Path;

/**
 * MariaDB/rocksDB is Apollo database. It shouдd be installed from apollo-mariadb package
 * or just be available by configured URL
 * This class tries to start MariaDB from installed package
 * @author Oleksiy Lukin alukin@gmail.com
 */
public class MariaDbStarter {
    private DbControl dbControl;
    private DbRunParams dbParams;
    
    public MariaDbStarter(Path dbDataDir, Path dbInstallDir) {
        
         Path confFile = Path.of("my-apl-user.conf");
        
         DbRunParams params = new MariaDbRunParamsBuilder()
                .dbConfigFile(confFile)
                .dbDataDir(dbDataDir)
                .dbInstallDir(dbInstallDir)
                .redirectErr(Path.of("maria_err.log"))
                .redirectOut(Path.of("maria_out.log"))
                .dbUser("apl")
                .dbPassword("apl")
                .build();       
        dbControl = new MariaDbControl(params);
    }
    
}
