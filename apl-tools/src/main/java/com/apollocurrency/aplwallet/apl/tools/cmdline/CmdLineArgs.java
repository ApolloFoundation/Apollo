/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.cmdline;

import com.beust.jcommander.Parameter;

/**
 * Command line arguments for cryptoutils
 * @author alukin@gmail.com
 * @see http://jcommander.org/
 */
public class CmdLineArgs {
    
    //Main parameter
    @Parameter(description = "[command arguments]")    
    public String command;
    
    @Parameter(names = {"--debug", "-d"}, description = "Debug mode")
    public boolean debug = false;
    @Parameter(names = {"--verbose", "-v"}, description = "Verbosity level 0-9")
    public Integer verbose = 1;
    @Parameter(names = "--help", help = true, description = "Print help message")
    public boolean help;   
    @Parameter(names = {"--ignore-resources"}, description = "Ignore resources bundled with application jar. Default is false")
    public boolean ingnoreResources = false;
    @Parameter(names = {"--resources-path", "-r"}, description = "Load all resources from specified path. Sytem resources ignored.")
    public String resourcesPath = "";
    @Parameter(names = {"--config-dir", "-c"}, description = "Load all configuration and resources from specified path. Sytem resources not ignored, standard config search is ignored.")
    public String configDir = "";
    @Parameter(names = {"--log-dir", "-l"}, description = "Save log files to from specified directory.")
    public String logDir = "";
    @Parameter(names = {"--db-dir"}, description = "Load/Save DB files to from specified directory. Ignored if DB URL is remote.")
    public String dbDir = "";
    @Parameter(names = {"--vault-key-dir"}, description = "Load/Save vault wallets keys to/form specified keystore directory.")
    public String vaultKeystoreDir = "";
     
    public boolean isResourceIgnored() {
        return !resourcesPath.isEmpty() || ingnoreResources;
    }    
}
