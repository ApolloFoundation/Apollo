/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools.cmdline;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author alukin@gmail.com
 */
@Parameters(commandDescription = "Encrypt/Decrypt Updater URL. Default ot decrypt")
public class UpdaterUrlCmd {
    public static final String CMD="updaterurl"; 
    @Parameter(description = "[input strings]")
    public List<String> parameters = new ArrayList<>();    
    @Parameter(names = {"--hex", "-x"}, description = "Use HEX format")
    public boolean useHex=false;
    @Parameter(names = {"--encrypt", "-e"}, description = "Encrypt. Default to decrypt")
    public boolean encrypt=false;
    @Parameter(names = {"--key", "-k"}, description = "Path to private key for decryption or to certificate for encryption")
    public String keyfile="key.pem";
    @Parameter(names = {"--in", "-i"}, description = "Input file. Argument string id used if ommited")
    public String infile="key.pem";   
}
