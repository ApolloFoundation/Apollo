package com.apollocurrency.aplwallet.apl.exec;

import picocli.CommandLine.Option;

/*
 *  Copyright © 2018-2021 Apollo Foundation
 * Command line parameters
 *
 * @author alukin@gmail.com
 * @author tx_hv@ukr.net
 */
public class CmdLineArgs {

    public static int DEFAULT_DEBUG_LEVEL = 2;

    @Option(names = {"--debug", "-d"}, description = "Debug level [0-4] from ERROR to TRACE")
    public int debug = DEFAULT_DEBUG_LEVEL;
    @Option(names = {"--debug-updater", "-du"}, description = "Force updater to use debug certificates for verifying update transactions")
    public boolean debugUpdater;
    @Option(names = {"--help", "-h"}, help = true, description = "Print help message")
    public boolean help;
    @Option(names = {"--service-mode", "-s"}, help = true, description = "Run in service mode with current system user")
    public boolean serviceMode;
    @Option(names = {"--ignore-resources"}, description = "Ignore resources bundled with application jar. Default is false")
    public boolean ingnoreResources = false;
    @Option(names = {"--config-dir", "-c"}, description = "Load all configuration and resources from specified path. System resources not ignored, standard config search is ignored.")
    public String configDir = "";
    @Option(names = {"--log-dir", "-l"}, description = "Save log files to from specified directory.")
    public String logDir = "";
    @Option(names = {"--db-dir"}, description = "Load/Save DB files to from specified directory. Ignored if DB URL is remote.")
    public String dbDir = "";
    @Option(names = {"--vault-key-dir"}, description = "Load/Save vault wallets keys to/form specified keystore directory.")
    public String vaultKeystoreDir = "";
    @Option(names = {"--dex-key-dir"}, description = "Load/Save dex keys to/form specified keystore directory.")
    public String dexKeystoreDir = "";
    @Option(names = {"--no-shard-import"}, description = "Start from Genesis block, do not try to import last shard")
    public boolean noShardImport = false;
    @Option(names = {"--no-shard-create"}, description = "Do not create shards even if it configured to do so. Shards require much more resources")
    public boolean noShardCreate = false;

    @Option(names = {"--update-attachment-file", "-u"}, description = "Full path to file which represent json of UpdateAttachment for local updates debug")
    public String updateAttachmentFile = "";

    //    TODO cleanup apl-default.properties
    @Option(names = {"--2fa-dir"}, description = "Load/Save 2FA keys to/form specified directory. Note that this Option will not work when you do not set apl.store2FAInFileSystem=true in apl-default.properties")
    public String twoFactorAuthDir = "";
    @Option(names = {"--dexp-dir"}, description = "Export/Import CSV data to/form specified directory.")
    public String dataExportDir = "";
    @Option(names = {"--pid-file"}, description = "Save PID to specified file.")
    public String pidFile = "";
    @Option(names = {"--net", "-n"}, help = true, description = "Connect to net [0-4]. 0 means mainnet, 1 - 1st testnet and so on")
    public int netIdx = -1;
    @Option(names = {"--chain", "-C"}, help = true, description = "Connect to net with given chainID. UUID of chain id may be specified partially, 6 symbos min. Configs must be present.")
    public String chainId = "";
    @Option(names = {"--testnet"}, help = true, description = "Connect to testent 1. Has higher priority then --net")
    public boolean isTestnet = false;
    //---
    @Option(names = {"--disable-weld-concurrent-deployment"},
        description = "If use it, Weld doesn't use ConcurrentDeployer and ConcurrentValidator to build the container. Default value is true.")
    public boolean disableWeldConcurrentDeployment = false;

    public boolean isResourceIgnored() {
        return ingnoreResources;
    }

    public int getNetIdx() {
        if (isTestnet) {
            netIdx = 1;
        }
        return netIdx;
    }
}
