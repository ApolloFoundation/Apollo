/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.tools.impl.UpdaterUrlUtils;
import com.apollocurrency.aplwallet.apl.tools.impl.MintWorker;
import com.apollocurrency.aplwallet.apl.tools.impl.SignTransactions;
import com.apollocurrency.aplwallet.apl.tools.impl.HeightMonitor;
import com.apollocurrency.aplwallet.apl.tools.impl.GeneratePublicKey;
import com.apollocurrency.aplwallet.apl.tools.impl.ConstantsExporter;
import com.apollocurrency.aplwallet.apl.tools.impl.BaseTarget;
import com.apollocurrency.aplwallet.apl.tools.impl.CompactDatabase;
import com.apollocurrency.aplwallet.apl.core.app.AplCore;
import com.apollocurrency.aplwallet.apl.core.app.Constants;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainUtils;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainsConfigHolder;
import com.apollocurrency.aplwallet.apl.tools.cmdline.BaseTargetCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CmdLineArgs;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CompactDbCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.ConstantsCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.HeightMonitorCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.MintCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.PubKeyCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.SignTxCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.UpdaterUrlCmd;
import com.apollocurrency.aplwallet.apl.util.cdi.AplContainer;
import com.apollocurrency.aplwallet.apl.util.env.EnvironmentVariables;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.apollocurrency.aplwallet.apl.util.env.RuntimeEnvironment;
import com.apollocurrency.aplwallet.apl.util.env.config.Chain;
import com.apollocurrency.aplwallet.apl.util.env.config.ChainsConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.config.PropertiesConfigLoader;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProvider;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.DirProviderFactory;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.PredefinedDirLocations;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;
import com.beust.jcommander.JCommander;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point to all Apollo tools. This is Swiss Army Knife for all Apollo
 * utilites with comprehensive command lline interface
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
    private static final UpdaterUrlCmd urlcmd = new UpdaterUrlCmd();
    private static final ConstantsCmd constcmd = new ConstantsCmd();
    private static final BaseTargetCmd basetarget = new BaseTargetCmd();
    private ApolloTools toolsApp;
    private static AplContainer container;

    private PropertiesHolder propertiesHolder;
    public static DirProvider dirProvider;
    private static final List<String> SYSTEM_PROPERTY_NAMES = Arrays.asList(
            "socksProxyHost",
            "socksProxyPort",
            "apl.enablePeerUPnP");

    private static DirProvider createDirProvider(Map<UUID, Chain> chains, PredefinedDirLocations dirLocations, boolean isService) {
        UUID chainId = ChainUtils.getActiveChain(chains).getChainId();
        return new DirProviderFactory().getInstance(isService, chainId, Constants.APPLICATION_DIR_NAME, dirLocations);
    }

    public static PredefinedDirLocations merge(CmdLineArgs args, EnvironmentVariables vars) {
        return new PredefinedDirLocations(
                StringUtils.isBlank(args.dbDir) ? vars.dbDir : args.dbDir,
                StringUtils.isBlank(args.logDir) ? vars.logDir : args.logDir,
                StringUtils.isBlank(args.vaultKeystoreDir) ? vars.vaultKeystoreDir : args.vaultKeystoreDir,
                "",
                ""
        );
    }

    private void initCDI() {
     //   RuntimeEnvironment.getInstance().setMain(ApolloTools.class);
        EnvironmentVariables envVars = new EnvironmentVariables(Constants.APPLICATION_DIR_NAME);
        ConfigDirProvider configDirProvider = new ConfigDirProviderFactory().getInstance(false, Constants.APPLICATION_DIR_NAME);

        PropertiesConfigLoader propertiesLoader = new PropertiesConfigLoader(
                configDirProvider,
                args.isResourceIgnored(),
                com.apollocurrency.aplwallet.apl.util.StringUtils.isBlank(args.configDir) ? envVars.configDir : args.configDir,
                Constants.APPLICATION_DIR_NAME + ".properties",
                SYSTEM_PROPERTY_NAMES);

        ChainsConfigLoader chainsConfigLoader = new ChainsConfigLoader(
                configDirProvider,
                args.isResourceIgnored(),
                com.apollocurrency.aplwallet.apl.util.StringUtils.isBlank(args.configDir) ? envVars.configDir : args.configDir,
                "chains.json");
        Map<UUID, Chain> chains = chainsConfigLoader.load();
        dirProvider = createDirProvider(chains, merge(args, envVars), false);

        container = AplContainer.builder().containerId("APL-TOOLS-CDI")
                .recursiveScanPackages(AplCore.class)
                .recursiveScanPackages(PropertiesHolder.class)
                .annotatedDiscoveryMode().build();

        toolsApp.propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
        toolsApp.propertiesHolder.init(propertiesLoader.load());
        
        BlockchainConfig blockchainConfig = CDI.current().select(BlockchainConfig.class).get();
        ChainsConfigHolder chainsConfigHolder = CDI.current().select(ChainsConfigHolder.class).get();
        chainsConfigHolder.setChains(chains);
        blockchainConfig.updateChain(chainsConfigHolder.getActiveChain());
    }

    public static String join(Collection collection, String delimiter) {
        String res = collection.stream()
                               .map(Object::toString)
                               .collect(Collectors.joining(delimiter)).toString();
        return res;
    }
    
    public static String readFile(String path) throws IOException{
       String res = new String(Files.readAllBytes(Paths.get(path)));
       return res;
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
            input = join(urlcmd.parameters," ");
        }else{
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
    
    private int baseTarget(){
       int height = 1000;
       if(!basetarget.parameters.isEmpty()){
           try{
              height = Integer.parseInt(basetarget.parameters.get(0));
           }catch(NumberFormatException e){
              System.err.println("Invalid height: "+basetarget.parameters.get(0));
              return PosixExitCodes.EX_USAGE.exitCode();
           }
       }
       return BaseTarget.doCalcualte(0);
    }
    
    public static void main(String[] argv) {
        ApolloTools toolsApp = new ApolloTools();
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .addCommand(CompactDbCmd.CMD, compactDb)
                .addCommand(MintCmd.CMD, mint)
                .addCommand(HeightMonitorCmd.CMD, heightMonitor)
                .addCommand(PubKeyCmd.CMD, pubkey)
                .addCommand(SignTxCmd.CMD, signtx)
                .addCommand(UpdaterUrlCmd.CMD, urlcmd)
                .addCommand(ConstantsCmd.CMD, constcmd)
                .addCommand(BaseTargetCmd.CMD, basetarget)
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
            toolsApp.initCDI();
            System.exit(toolsApp.compactDB());
        } else if (jc.getParsedCommand().equalsIgnoreCase(MintCmd.CMD)) {
            toolsApp.initCDI();
            System.exit(toolsApp.mint());
        } else if (jc.getParsedCommand().equalsIgnoreCase(HeightMonitorCmd.CMD)) {
            System.exit(toolsApp.heightMonitor());
        } else if (jc.getParsedCommand().equalsIgnoreCase(PubKeyCmd.CMD)) {
            System.exit(toolsApp.pubkey());
        } else if (jc.getParsedCommand().equalsIgnoreCase(SignTxCmd.CMD)) {
            System.exit(toolsApp.signtx());
        } else if (jc.getParsedCommand().equalsIgnoreCase(UpdaterUrlCmd.CMD)) {
            System.exit(toolsApp.updaterUrlOp());
        } else if (jc.getParsedCommand().equalsIgnoreCase(ConstantsCmd.CMD)) {
            System.exit(ConstantsExporter.export(constcmd.outfile));
        } else if (jc.getParsedCommand().equalsIgnoreCase(BaseTargetCmd.CMD)) {
            System.exit(toolsApp.baseTarget());
        }

    }
}
