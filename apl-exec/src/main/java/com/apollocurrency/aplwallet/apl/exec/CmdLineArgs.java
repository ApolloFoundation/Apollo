package com.apollocurrency.aplwallet.apl.exec;

import com.beust.jcommander.Parameter;

/**
 *
 * @author alukin@gmail.com
 */
public class CmdLineArgs {
    @Parameter(names = {"--debug", "-d"}, description = "Debug mode")
    public boolean debug = false;
    @Parameter(names = {"--verbose", "-v"}, description = "Verbosity level 0-9")
    public Integer verbose = 1;
    @Parameter(names = "--help", help = true, description = "Print help message")
    public boolean help;
    @Parameter(names = {"--ignore-resources"}, description = "Ignore resources bundled with application jar. Default is false")
    public boolean ingnoreResources = false;
    @Parameter(names = {"--resources-path","-r"}, description = "Load all resources from specified path. Sytem resources ignored.")
    public String resourcesPath="";        
     
}
