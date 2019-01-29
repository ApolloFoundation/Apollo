/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools;

import com.apollocurrency.aplwallet.apl.tools.cmdline.CmdLineArgs;
import com.apollocurrency.aplwallet.apl.tools.cmdline.CompactDbCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.HeihgtMonitorCmd;
import com.apollocurrency.aplwallet.apl.tools.cmdline.MintCmd;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;
import com.beust.jcommander.JCommander;

/**
 * Main entry point to all tools
 * @author alukin@gmail.com
 */
public class ApolloTools {
    
   public static void main(String[] argv) {
       ApolloTools tools = new ApolloTools();
        CmdLineArgs args = new CmdLineArgs();
        CompactDbCmd compactDb = new CompactDbCmd();
        MintCmd mint = new MintCmd();
        HeihgtMonitorCmd heightMonitor = new HeihgtMonitorCmd();
        
        JCommander jc = JCommander.newBuilder()
                .addObject(args)
                .addCommand("compctdb", compactDb)
                .addCommand("mint", mint)
                .addCommand("heightmonitor", heightMonitor)
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
        }
   } 
}
