/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.tools.cmdline.CmdLineArgs;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CompactDbCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.HeightMonitorCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.MintCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.PubKeyCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.SignTxCmd;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point to all Apollo tools
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
   
   private void initCDI(){
       
   }
   private int compactDB(){
       return 0;
   }
   private int mint(){
       return 0;
   }
   private int heightMonitor(){
       return 0;
   }
   private int pubkey(){
       return 0;
   }
   private int signtx(){
       return 0;
   }   
   public static void main(String[] argv) {
       ApolloTools toolsApp = new ApolloTools();
       JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .addCommand(CompactDbCmd.COMPACT_DB_CMD, compactDb)
                .addCommand(MintCmd.MINT_CMD, mint)
                .addCommand(HeightMonitorCmd.HEIGHT_MONITOR_CMD, heightMonitor)
                .addCommand(PubKeyCmd.PUB_KEY_CMD,pubkey)
                .addCommand(SignTxCmd.SIGN_TX_CMD,signtx)
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
        if (args.help || argv.length==0) {
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
