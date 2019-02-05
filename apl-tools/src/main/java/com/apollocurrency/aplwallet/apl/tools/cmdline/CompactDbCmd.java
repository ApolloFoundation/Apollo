/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

/**
 *
 * @author alukin@gmail.com
 */
@Parameters(commandDescription = "Compact database")
public class CompactDbCmd {
   public static final String CMD="compactdb";
   @Parameter(names = {"--chainId", "-c"}, description = "Chain ID")
   public String chainID=""; 
}
