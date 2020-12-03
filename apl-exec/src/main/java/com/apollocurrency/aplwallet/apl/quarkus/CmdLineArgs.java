/*
 * Copyright © 2018-2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.quarkus;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command
public class CmdLineArgs implements Runnable {

    public static int DEFAULT_DEBUG_LEVEL = 2;

    @CommandLine.Option(names = {"--debug", "-d"}, defaultValue = "2",
        description = "Debug level [0-4] from ERROR to TRACE")
    public int debug = DEFAULT_DEBUG_LEVEL;

    @CommandLine.Option(names = {"-du", "--debug-updater"}, description = "Force updater to use debug certificates for verifying update transactions")
    public boolean debugUpdater;

    @CommandLine.Option(names = {"-s", "--service-mode"}, description = "Run in service mode with current system user")
    public boolean serviceMode;

    @CommandLine.Option(names = "--ignore-resources", description = "Ignore resources bundled with application jar. Default is false")
    public boolean ignoreResources = false;

    @CommandLine.Option(names = {"-c", "--config-dir"}, description = "Load all configuration and resources from specified path. System resources not ignored, standard config search is ignored.")
    public String configDir;

    @CommandLine.Option(names = {"-l", "--log-dir"}, description = "Save log files to from specified directory.")
    public String logDir;

    @CommandLine.Option(names = "--db-dir", description = "Load/Save DB files to from specified directory. Ignored if DB URL is remote.")
    public String dbDir;

    @CommandLine.Option(names = "--vault-key-dir", description = "Load/Save vault wallets keys to/form specified keystore directory.")
    public String vaultKeystoreDir = "";

    @CommandLine.Option(names = "--dex-key-dir", description = "Load/Save dex keys to/form specified keystore directory.")
    public String dexKeystoreDir = "";

    @CommandLine.Option(names = "--no-shard-import", defaultValue = "true", fallbackValue = "true", negatable = true,
        description = "Start from Genesis block, do not try to import last shard")
    public Boolean noShardImport = true;

    @CommandLine.Option(names = "--no-shard-create", defaultValue = "true", fallbackValue = "true", negatable = true,
        description = "Do not create shards even if it configured to do so. Shards require much more resources")
    public Boolean noShardCreate = Boolean.TRUE;

    @CommandLine.Option(names = {"-u", "--update-attachment-file"}, description = "Full path to file which represent json of UpdateAttachment for local updates debug")
    public String updateAttachmentFile = "";

    @CommandLine.Option(names = "--2fa-dir", description = "Load/Save 2FA keys to/form specified directory. Note that this parameter will not work when you do not set apl.store2FAInFileSystem=true in apl-default.properties")
    public String twoFactorAuthDir = "";

    @CommandLine.Option(names = "--dexp-dir", description = "Export/Import CSV data to/form specified directory.")
    public String dataExportDir = "";

    @CommandLine.Option(names = "--pid-file", description = "Save PID to specified file.")
    public String pidFile = "";

    @CommandLine.Option(names = {"--start-mint", "-m"}, description = "Start currency minting worker")
    public boolean startMint;

    @CommandLine.Option(names = {"-n", "--net"},
        description = "Connect to net [0-4]. 0 means mainnet, 1 - 1st testnet and so on")
    public int netIdx = -1;

    @CommandLine.Option(names = {"-C", "--chain"},
        description = "Connect to net with given chainID. UUID of chain id may be specified partially, 6 symbos min. Configs must be present.")
    public String chainId = "";

    @CommandLine.Option(names = "--testnet", defaultValue = "false",
        description = "Connect to testnet 1. Has higher priority then --net")
    public boolean isTestnet;

    @CommandLine.Option(names = {"--help", "-h"}, usageHelp = true, description = "Print help message")
    public boolean help;

    public boolean exit = false;

    @Override
    public void run() {
        System.out.println("==== Doing something after command line parsing ====");
        System.out.println("==== Name: " + this.toString());
        if(help){
            System.out.println("==== HELP ====");
            CommandLine.usage(this, System.out);
            exit = true;
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CmdLineArgs{");
        sb.append("debug=").append(debug);
        sb.append(", debugUpdater=").append(debugUpdater);
        sb.append(", serviceMode=").append(serviceMode);
        sb.append(", ignoreResources=").append(ignoreResources);
        sb.append(", configDir='").append(configDir).append('\'');
        sb.append(", logDir='").append(logDir).append('\'');
        sb.append(", dbDir='").append(dbDir).append('\'');
        sb.append(", vaultKeystoreDir='").append(vaultKeystoreDir).append('\'');
        sb.append(", dexKeystoreDir='").append(dexKeystoreDir).append('\'');
        sb.append(", noShardImport=").append(noShardImport);
        sb.append(", noShardCreate=").append(noShardCreate);
        sb.append(", updateAttachmentFile='").append(updateAttachmentFile).append('\'');
        sb.append(", twoFactorAuthDir='").append(twoFactorAuthDir).append('\'');
        sb.append(", dataExportDir='").append(dataExportDir).append('\'');
        sb.append(", pidFile='").append(pidFile).append('\'');
        sb.append(", startMint=").append(startMint);
        sb.append(", netIdx=").append(netIdx);
        sb.append(", chainId='").append(chainId).append('\'');
        sb.append(", isTestnet=").append(isTestnet);
        sb.append(", help=").append(help);
        sb.append('}');
        return sb.toString();
    }

    public static void printFields(Class klazz)  {
        Field[] fields = klazz.getDeclaredFields();
        System.out.printf("%d fields:%n", fields.length);
        for (Field field : fields) {
            System.out.printf("%s %s %s%n",
                Modifier.toString(field.getModifiers()),
                field.getType().getSimpleName(),
                field.getName()
            );
        }
    }

    /*
    public int debug = DEFAULT_DEBUG_LEVEL;
    @CommandLine.Option(names = {"--debug", "-d"}, defaultValue = "2",
        description = "Debug level [0-4] from ERROR to TRACE")
    public void setDebug(int debug) {
        this.debug = debug;
    }

    public boolean debugUpdater;
    @CommandLine.Option(names = {"-du", "--debug-updater"}, description = "Force updater to use debug certificates for verifying update transactions")
    public void setDebugUpdater(boolean debugUpdater) {
        this.debugUpdater = debugUpdater;
    }

    public boolean serviceMode;
    @CommandLine.Option(names = {"-s", "--service-mode"}, description = "Run in service mode with current system user")
    public void setServiceMode(boolean serviceMode) {
        this.serviceMode = serviceMode;
    }

    public boolean ignoreResources = false;
    @CommandLine.Option(names = "--ignore-resources", description = "Ignore resources bundled with application jar. Default is false")
    public void setIgnoreResources(boolean ignoreResources) {
        this.ignoreResources = ignoreResources;
    }

    public String configDir;
    @CommandLine.Option(names = {"-c", "--config-dir"}, description = "Load all configuration and resources from specified path. System resources not ignored, standard config search is ignored.")
    public void setConfigDir(String configDir) {
        this.configDir = configDir;
    }

    public String logDir;
    @CommandLine.Option(names = {"-l", "--log-dir"}, description = "Save log files to from specified directory.")
    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public String dbDir;
    @CommandLine.Option(names = "--db-dir", description = "Load/Save DB files to from specified directory. Ignored if DB URL is remote.")
    public void setDbDir(String dbDir) {
        this.dbDir = dbDir;
    }

    public String vaultKeystoreDir = "";
    @CommandLine.Option(names = "--vault-key-dir", description = "Load/Save vault wallets keys to/form specified keystore directory.")
    public void setVaultKeystoreDir(String vaultKeystoreDir) {
        this.vaultKeystoreDir = vaultKeystoreDir;
    }

    public String dexKeystoreDir = "";
    @CommandLine.Option(names = "--dex-key-dir", description = "Load/Save dex keys to/form specified keystore directory.")
    public void setDexKeystoreDir(String dexKeystoreDir) {
        this.dexKeystoreDir = dexKeystoreDir;
    }

    public Boolean noShardImport = true;
    @CommandLine.Option(names = "--no-shard-import", defaultValue = "true", fallbackValue = "true", negatable = true,
        description = "Start from Genesis block, do not try to import last shard")
    public void setNoShardImport(Boolean noShardImport) {
        this.noShardImport = noShardImport;
    }

    public Boolean noShardCreate = Boolean.TRUE;
    @CommandLine.Option(names = "--no-shard-create", defaultValue = "true", fallbackValue = "true", negatable = true,
        description = "Do not create shards even if it configured to do so. Shards require much more resources")
    public void setNoShardCreate(Boolean noShardCreate) {
        this.noShardCreate = noShardCreate;
    }

    public String updateAttachmentFile = "";
    @CommandLine.Option(names = {"-u", "--update-attachment-file"}, description = "Full path to file which represent json of UpdateAttachment for local updates debug")
    public void setUpdateAttachmentFile(String updateAttachmentFile) {
        this.updateAttachmentFile = updateAttachmentFile;
    }

    public String twoFactorAuthDir = "";
    @CommandLine.Option(names = "--2fa-dir", description = "Load/Save 2FA keys to/form specified directory. Note that this parameter will not work when you do not set apl.store2FAInFileSystem=true in apl-default.properties")
    public void setTwoFactorAuthDir(String twoFactorAuthDir) {
        this.twoFactorAuthDir = twoFactorAuthDir;
    }

    public String dataExportDir = "";
    @CommandLine.Option(names = "--dexp-dir", description = "Export/Import CSV data to/form specified directory.")
    public void setDataExportDir(String dataExportDir) {
        this.dataExportDir = dataExportDir;
    }

    public String pidFile = "";
    @CommandLine.Option(names = "--pid-file", description = "Save PID to specified file.")
    public void setPidFile(String pidFile) {
        this.pidFile = pidFile;
    }

    public boolean startMint;
    @CommandLine.Option(names = {"--start-mint", "-m"}, description = "Start currency minting worker")
    public void setStartMint(boolean startMint) {
        this.startMint = startMint;
    }

    public int netIdx = -1;
    @CommandLine.Option(names = {"-n", "--net"},
        description = "Connect to net [0-4]. 0 means mainnet, 1 - 1st testnet and so on")
    public void setNetIdx(int netIdx) {
        this.netIdx = netIdx;
    }

    public String chainId = "";
    @CommandLine.Option(names = {"-C", "--chain"},
        description = "Connect to net with given chainID. UUID of chain id may be specified partially, 6 symbos min. Configs must be present.")
    public void setChainId(String chainId) {
        this.chainId = chainId;
    }

    public boolean isTestnet;
    @CommandLine.Option(names = "--testnet", defaultValue = "false",
        description = "Connect to testnet 1. Has higher priority then --net")
    public void setTestnet(boolean testnet) {
        isTestnet = testnet;
    }
*/

}
