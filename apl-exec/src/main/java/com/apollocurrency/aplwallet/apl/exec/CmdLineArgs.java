package com.apollocurrency.aplwallet.apl.exec;

import com.beust.jcommander.Parameter;

/**
 * Command line parameters
 *
 * @author alukin@gmail.com
 */
public class CmdLineArgs {
    @Parameter(names = {"--debug", "-d"}, description = "Debug mode")
    public boolean debug = false;
    @Parameter(names = {"--verbose", "-v"}, description = "Verbosity level 0-9")
    public Integer verbose = 1;
    @Parameter(names = {"--help", "-h"}, help = true, description = "Print help message")
    public boolean help;
    @Parameter(names = {"--service-mode", "-s"}, help = true, description = "Run in service mode with current system user")
    public boolean serviceMode;
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
    //    TODO cleanup apl-default.properties
    @Parameter(names = {"--2fa-dir"}, description = "Load/Save 2FA keys to/form specified directory. Note that this parameter will not work " +
            "when you do not set apl.store2FAInFileSystem=true in apl-default.properties")
    public String twoFactorAuthDir = "";
    @Parameter(names = {"--pid-file"}, description = "Save PID to specified file.")
    public String pidFile = "";
    @Parameter(names = {"--start-mint", "-m"}, help = true, description = "Start currency minting worker")
    public boolean startMint;
    
    public boolean isResourceIgnored() {
        return !resourcesPath.isEmpty() || ingnoreResources;
    }
}
