/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainIdServiceImpl;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CmdLineArgs;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CompactDbCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.HeightMonitorCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.MintCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.PubKeyCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.SignTxCmd;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import com.apollocurrency.aplwallet.apl.util.env.EnvironmentVariables;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.apollocurrency.aplwallet.apl.util.env.PropertiesLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.util.UUID;
import javax.enterprise.inject.spi.CDI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point to all Apollo tools
 *
 * @author alukin@gmail.com
 */
public class ApolloTools {

    private static final Logger log = LoggerFactory.getLogger(ApolloTools.class);
    private static final CmdLineArgs args = new CmdLineArgs();
    private static final CompactDbCmd compactDb = new CompactDbCmd();
    private static final MintCmd mint = new MintCmd();
    private static final HeightMonitorCmd heightMonitor = new HeightMonitorCmd();
    private static final PubKeyCmd pubkey = new PubKeyCmd();
    private static final SignTxCmd signtx = new SignTxCmd();
    private ApolloTools toolsApp;
    private static AplContainer container;
    private static PropertiesLoader propertiesLoader;
    private PropertiesHolder propertiesHolder;
    public static DirProvider dirProvider;
    
    private static DirProvider createDirProvider(PredefinedDirLocations dirLocations, boolean isService) {
        try {
            ChainIdServiceImpl chainIdService = new ChainIdServiceImpl();
            UUID chainId = chainIdService.getActiveChain().getChainId();
            return new DirProviderFactory().getInstance(isService, chainId, Constants.APPLICATION_DIR_NAME, dirLocations);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create dirProvider, cannot load chains config", e);
        }
    }
    
    public static PredefinedDirLocations merge(CmdLineArgs args, EnvironmentVariables vars) {
        return new PredefinedDirLocations(
                StringUtils.isBlank(args.dbDir)            ? vars.dbDir            : args.dbDir,
                StringUtils.isBlank(args.logDir)           ? vars.logDir           : args.logDir,
                StringUtils.isBlank(args.vaultKeystoreDir) ? vars.vaultKeystoreDir : args.vaultKeystoreDir,
                "",
                ""
        );
    }
    
    private void initCDI() {
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);
        EnvironmentVariables environmentVariables = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);
        dirProvider = createDirProvider(merge(args,environmentVariables), false);        
       
        propertiesLoader = new PropertiesLoader(configDirProvider,
                args.isResourceIgnored(),
                args.configDir);        
        container = AplContainer.builder().containerId("APL-TOOLS-CDI")
                .recursiveScanPackages(AplCore.class)
                .recursiveScanPackages(PropertiesHolder.class)
                .annotatedDiscoveryMode().build();
        toolsApp.propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        toolsApp.propertiesHolder.init(propertiesLoader.getProperties());
    }

    private int compactDB() {
        CompactDatabase cdb = new CompactDatabase();
        cdb.init();
        return cdb.compactDatabase();
    }

    private int mint() {
        MintWorker mintWorker = new MintWorker();
        //TODO: exit code
        mintWorker.mint();        
        return 0;
    }

    private int heightMonitor() {
//TODO: command line parameters        
        HeightMonitor hm = HeightMonitor.create(null, null, null);
        hm.start();
        //TODO: exit code        
        return 0;
    }

    private int pubkey() {
        return 0;
    }

    private int signtx() {
        return 0;
    }

    public static void main(String[] argv) {
        ApolloTools toolsApp = new ApolloTools();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .addCommand(CompactDbCmd.COMPACT_DB_CMD, compactDb)
                .addCommand(MintCmd.MINT_CMD, mint)
                .addCommand(HeightMonitorCmd.HEIGHT_MONITOR_CMD, heightMonitor)
                .addCommand(PubKeyCmd.PUB_KEY_CMD, pubkey)
                .addCommand(SignTxCmd.SIGN_TX_CMD, signtx)
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
        } else if (jc.getParsedCommand().equalsIgnoreCase(CompactDbCmd.COMPACT_DB_CMD)) {
            toolsApp.initCDI();
            System.exit(toolsApp.compactDB());
        } else if (jc.getParsedCommand().equalsIgnoreCase(MintCmd.MINT_CMD)) {
            toolsApp.initCDI();
            System.exit(toolsApp.mint());
        } else if (jc.getParsedCommand().equalsIgnoreCase(HeightMonitorCmd.HEIGHT_MONITOR_CMD)) {
            toolsApp.initCDI();
            System.exit(toolsApp.heightMonitor());
        } else if (jc.getParsedCommand().equalsIgnoreCase(PubKeyCmd.PUB_KEY_CMD)) {
            toolsApp.initCDI();
            System.exit(toolsApp.pubkey());
        } else if (jc.getParsedCommand().equalsIgnoreCase(SignTxCmd.SIGN_TX_CMD)) {
            toolsApp.initCDI();
            System.exit(toolsApp.signtx());
        }

    }
}
