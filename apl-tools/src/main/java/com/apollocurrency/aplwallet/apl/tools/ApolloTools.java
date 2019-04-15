/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools;

import static org.slf4j.LoggerFactory.getLogger;

import com.apollocurrency.aplwallet.apl.tools.cmdline.CmdLineArgs;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CompactDbCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.ConstantsCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.HeightMonitorCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.PubKeyCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.SignTxCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.UpdaterUrlCmd;
import com.apollocurrency.aplwallet.apl.tools.impl.CompactDatabase;
import com.apollocurrency.aplwallet.apl.tools.impl.ConstantsExporter;
import com.apollocurrency.aplwallet.apl.tools.impl.GeneratePublicKey;
import com.apollocurrency.aplwallet.apl.tools.impl.SignTransactions;
import com.apollocurrency.aplwallet.apl.tools.impl.UpdaterUrlUtils;
import com.apollocurrency.aplwallet.apl.tools.impl.heightmon.HeightMonitor;
import com.apollocurrency.aplwallet.apl.util.Constants;
import com.apollocurrency.aplwallet.apl.util.env.EnvironmentVariables;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainUtils;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.beust.jcommander.JCommander;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Main entry point to all Apollo tools. This is Swiss Army Knife for all Apollo
 * utilites with comprehensive command lline interface
 *
 * @author alukin@gmail.com
 */
public class ApolloTools {

    private static Logger log;
    private static final CmdLineArgs args = new CmdLineArgs();
    private static final CompactDbCmd compactDb = new CompactDbCmd();
    private static final HeightMonitorCmd heightMonitorCmd = new HeightMonitorCmd();
    private static final PubKeyCmd pubkey = new PubKeyCmd();
    private static final SignTxCmd signtx = new SignTxCmd();
    private static final UpdaterUrlCmd urlcmd = new UpdaterUrlCmd();
    private static final ConstantsCmd constcmd = new ConstantsCmd();
    
    private static ApolloTools toolsApp;
    private Chain activeChain;
    private Map<UUID, Chain> chains;
    private PredefinedDirLocations dirLocations;

    private PropertiesHolder propertiesHolder;
    private DirProvider dirProvider;

    private static final List<String> SYSTEM_PROPERTY_NAMES = Arrays.asList(
            "socksProxyHost",
            "socksProxyPort",
            "apl.enablePeerUPnP");

    public static PredefinedDirLocations merge(CmdLineArgs args, EnvironmentVariables vars) {
        return new PredefinedDirLocations(
                StringUtils.isBlank(args.dbDir) ? vars.dbDir : args.dbDir,
                StringUtils.isBlank(args.logDir) ? vars.logDir : args.logDir,
                StringUtils.isBlank(args.vaultKeystoreDir) ? vars.vaultKeystoreDir : args.vaultKeystoreDir,
                "",
                ""
        );
    }

    private void readConfigs() {
        RuntimeEnvironment.getInstance().setMain(ApolloTools.class);
        EnvironmentVariables envVars = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);

        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                configDirProvider,
                args.isResourceIgnored(),
                StringUtils.isBlank(args.configDir) ? envVars.configDir : args.configDir,
                SYSTEM_PROPERTY_NAMES);

        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(
                configDirProvider,
                args.isResourceIgnored(),
                StringUtils.isBlank(args.configDir) ? envVars.configDir : args.configDir);
        chains = chainsConfigLoader.load();
        activeChain = ChainUtils.getActiveChain(chains);
        // dirProvider = createDirProvider(chains, merge(args, envVars), chainsConfigLoader.);
        dirLocations = merge(args, envVars);
        dirProvider = DirProviderFactory.getProvider(false, activeChain.getChainId(), Constants.APPLICATION_DIR_NAME, dirLocations);
        toolsApp.propertiesHolder = new PropertiesHolder();
        toolsApp.propertiesHolder.init(propertiesLoader.load());
        RuntimeEnvironment.getInstance().setDirProvider(dirProvider);
    }

    public static String join(Collection collection, String delimiter) {
        String res = collection.stream()
                .map(Object::toString)
                .collect(Collectors.joining(delimiter)).toString();
        return res;
    }

    public static String readFile(String path) throws IOException {
        String res = new String(Files.readAllBytes(Paths.get(path)));
        return res;
    }

    private int compactDB() {
        if (!compactDb.chainID.isEmpty()) {
            try {
                UUID blockchainId = UUID.fromString(compactDb.chainID);
                Chain c = chains.get(blockchainId);
                if(c==null){
                    System.out.println("Chain not coonfigured: "+compactDb.chainID);
                    return PosixExitCodes.EX_CONFIG.exitCode();
                }
                dirProvider = DirProviderFactory.getProvider(false, blockchainId, Constants.APPLICATION_DIR_NAME, dirLocations);
            } catch (IllegalArgumentException ex) {
                System.err.println("Can not convert chain ID " + compactDb.chainID + " to UUID");
                return PosixExitCodes.EX_CONFIG.exitCode();
            }
        }
        CompactDatabase cdb = new CompactDatabase(propertiesHolder, dirProvider);
        
        return cdb.compactDatabase();

    }

    private int heightMonitor() {
        try {
            String peerFile = heightMonitorCmd.peerFile;
            List<String> peerIps = Files.readAllLines(Paths.get(peerFile));
            HeightMonitor hm = new HeightMonitor(peerIps, heightMonitorCmd.intervals, heightMonitorCmd.frequency, heightMonitorCmd.port);
            hm.start();
            Runtime.getRuntime().addShutdownHook(new Thread(hm::stop));
            return 0;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int pubkey() {
        GeneratePublicKey.doInteractive();
        return 0;
    }

    private int signtx() {
        int res;
        if (signtx.useJson) {
            res = SignTransactions.signJson(signtx.infile, signtx.outfile);
        } else {
            res = SignTransactions.sign(signtx.infile, signtx.outfile);
        }
        return res;
    }

    private int updaterUrlOp() {
        int res;
        String input = "";
        if (urlcmd.infile.isEmpty()) {
            input = join(urlcmd.parameters, " ");
        } else {
            try {
                input = readFile(urlcmd.infile);
            } catch (IOException ex) {
                return PosixExitCodes.EX_OSFILE.exitCode();
            }
        }
        if (urlcmd.encrypt) {
            res = UpdaterUrlUtils.encrypt(urlcmd.keyfile, input, urlcmd.useHex);
        } else {
            res = UpdaterUrlUtils.decrypt(urlcmd.keyfile, input, !urlcmd.useHex);
        }
        return res;
    }


    public static void main(String[] argv) {
        log = getLogger(ApolloTools.class);
        toolsApp = new ApolloTools();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .addCommand(CompactDbCmd.CMD, compactDb)
                .addCommand(HeightMonitorCmd.CMD, heightMonitorCmd)
                .addCommand(PubKeyCmd.CMD, pubkey)
                .addCommand(SignTxCmd.CMD, signtx)
                .addCommand(UpdaterUrlCmd.CMD, urlcmd)
                .addCommand(ConstantsCmd.CMD, constcmd)
                .build();
        jc.setProgramName("apl-tools");
        try {
            jc.parse(argv);
        } catch (RuntimeException ex) {
            System.err.println("Error parsing command line arguments.");
            System.err.println(ex.getMessage());
            jc.usage();
            System.exit(PosixExitCodes.EX_USAGE.exitCode());
        }
        if (args.help || argv.length == 0) {
            jc.usage();
            System.exit(PosixExitCodes.OK.exitCode());
        }
        if (jc.getParsedCommand() == null) {
            jc.usage();
            System.exit(PosixExitCodes.OK.exitCode());
        } else if (jc.getParsedCommand().equalsIgnoreCase(CompactDbCmd.CMD)) {
            toolsApp.readConfigs();
            System.exit(toolsApp.compactDB());
        } else if (jc.getParsedCommand().equalsIgnoreCase(HeightMonitorCmd.CMD)) {
            toolsApp.heightMonitor();
        } else if (jc.getParsedCommand().equalsIgnoreCase(PubKeyCmd.CMD)) {
            System.exit(toolsApp.pubkey());
        } else if (jc.getParsedCommand().equalsIgnoreCase(SignTxCmd.CMD)) {
            System.exit(toolsApp.signtx());
        } else if (jc.getParsedCommand().equalsIgnoreCase(UpdaterUrlCmd.CMD)) {
            System.exit(toolsApp.updaterUrlOp());
        } else if (jc.getParsedCommand().equalsIgnoreCase(ConstantsCmd.CMD)) {
            System.exit(ConstantsExporter.export(constcmd.outfile));
        }

    }
}
