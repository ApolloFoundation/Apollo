/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.tools.cmdline.CmdLineArgs;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CompactDbCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.HeihgtMonitorCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.MintCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.PubKeyCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.SignTxCmd;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.beust.jcommander.JCommander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point to all tools
 * @author alukin@gmail.com
 */
public class ApolloTools {
   private static Logger log = LoggerFactory.getLogger(ApolloTools.class);
   
   public static void main(String[] argv) {
       ApolloTools tools = new ApolloTools();
       CmdLineArgs args = new CmdLineArgs();
       CompactDbCmd compactDb = new CompactDbCmd();
       MintCmd mint = new MintCmd();
       HeihgtMonitorCmd heightMonitor = new HeihgtMonitorCmd();
       PubKeyCmd pubkey = new PubKeyCmd();
       SignTxCmd signtx = new SignTxCmd(); 
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .addCommand(compactDb.COMPACT_DB_CMD, compactDb)
                .addCommand(mint.MINT_CMD, mint)
                .addCommand(heightMonitor.HEIGHT_MONITOR_CMD, heightMonitor)
                .addCommand(pubkey.PUB_KEY_CMD,pubkey)
                .addCommand(signtx.SIGN_TX_CMD,signtx)
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
        } else if (jc.getParsedCommand().equalsIgnoreCase("keystore")) {
            log.error("keystore functionality  is not implemented yet");
        }
   } 
}
