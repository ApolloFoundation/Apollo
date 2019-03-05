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
@Parameters(commandDescription = "Sign transaction")
public class SignTxCmd {
    public static final String CMD="signtx"; 
//    @Parameter(description = "{list of input files}")
//    public List<String> parameters = new ArrayList<>();
    @Parameter(names = {"--out", "-o"}, description = "Output path")
    public String outfile="out.txt";
    @Parameter(names = {"--input", "-i"}, description = "Input path")
    public String infile="in.txt";    
    @Parameter(names = {"--json", "-j"}, description = "Use JSON variant")
    public boolean useJson=false;     
}
