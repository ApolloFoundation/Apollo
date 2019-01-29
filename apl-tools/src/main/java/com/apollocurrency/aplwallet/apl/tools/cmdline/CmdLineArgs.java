/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.cmdline;

import com.beust.jcommander.Parameter;

/**
 * Command line arguments for cryptoutils
 * @author alukin@gmail.com
 * @see http://jcommander.org/
 */
public class CmdLineArgs {
    
    //Main parameter
    @Parameter(description = "Command to run. gencsr, savecert, genself")    
    public String command;
    
    @Parameter(names = {"--debug", "-d"}, description = "Debug mode")
    public boolean debug = false;
    @Parameter(names = {"--verbose", "-v"}, description = "Verbosity level 0-9")
    public Integer verbose = 1;
    @Parameter(names = "--help", help = true, description = "Print help message")
    public boolean help;
     @Parameter(names = {"--keypass", "-p"}, description = "Passphrase for private key encryption") //password = true)
    public String keypass;
    @Parameter(names = {"--storepass", "-s"}, description = "Passphrase for key store") //password = true)
    public String storepass;
    @Parameter(names = {"--storefile", "-f"}, description = "Path to key store file")
    public String storefile;
    @Parameter(names = {"--out", "-o"}, description = "Output path")
    public String outfile="";
     @Parameter(names = {"--input", "-i"}, description = "Input path")
    public String infile="newcert.pem";    
     
 //   @Parameter(description = "Files")
 //   public List<String> files = new ArrayList<>();

}
