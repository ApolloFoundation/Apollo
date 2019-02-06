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
@Parameters(commandDescription = "Export Apollo constants to JSON")
public class ConstantsCmd {
    public static final String CMD="constants"; 
    @Parameter(names = {"--out", "-o"}, description = "Output file")
    public String outfile="constants.json";   
}
