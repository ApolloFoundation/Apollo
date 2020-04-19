package com.apollocurrency.aplwallet.apl.exec;

import com.beust.jcommander.Parameter;

/**
 * Command line parameters
 *
 * @author alukin@gmail.com
 */
public class CmdLineArgs {
    public static int DEFAULT_DEBUG_LEVEL = 2;

    @Parameter(names = {"--debug", "-d"}, description = "Debug level [0-4] from ERROR to TRACE")
    public int debug = DEFAULT_DEBUG_LEVEL;
    @Parameter(names = {"--debug-updater", "-du"}, description = "Force updater to use debug certificates for verifying update transactions")
    public boolean debugUpdater;
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
    @Parameter(names = {"--dex-key-dir"}, description = "Load/Save dex keys to/form specified keystore directory.")
    public String dexKeystoreDir = "";
    @Parameter(names = {"--no-shard-import"}, description = "Start from Genesis block, do not try to import last shard", arity = 1)
    public Boolean noShardImport = null;
    @Parameter(names = {"--no-shard-create"}, description = "Do not create shards even if it configured to do so. Shards require much more resources", arity = 1)
    public Boolean noShardCreate = null;

    @Parameter(names = {"--update-attachment-file", "-u"}, description = "Full path to file which represent json of UpdateAttachment for local updates debug")
    public String updateAttachmentFile = "";

    //    TODO cleanup apl-default.properties
    @Parameter(names = {"--2fa-dir"}, description = "Load/Save 2FA keys to/form specified directory. Note that this parameter will not work when you do not set apl.store2FAInFileSystem=true in apl-default.properties")
    public String twoFactorAuthDir = "";
    @Parameter(names = {"--dexp-dir"}, description = "Export/Import CSV data to/form specified directory.")
    public String dataExportDir = "";
    @Parameter(names = {"--pid-file"}, description = "Save PID to specified file.")
    public String pidFile = "";
    @Parameter(names = {"--start-mint", "-m"}, help = true, description = "Start currency minting worker")
    public boolean startMint;
    @Parameter(names = {"--net", "-n"}, help = true, description = "Connect to net [0-3]. 0 means mainnet, 1 - 1st testnet and so on")
    public int netIdx = 0;
    @Parameter(names = {"--testnet"}, help = true, description = "Connect to testent 1. Has higher priority then --net")
    public boolean isTestnet = false;
    //---
    @Parameter(names = {"--disable-weld-concurrent-deployment"},
        description = "If use it, Weld doesn't use ConcurrentDeployer and ConcurrentValidator to build the container. Default value is true.")
    public boolean disableWeldConcurrentDeployment = false;

    public boolean isResourceIgnored() {
        return !resourcesPath.isEmpty() || ingnoreResources;
    }

    public int getNetIdx() {
        if (isTestnet) {
            netIdx = 1;
        }
        return netIdx;
    }
}
