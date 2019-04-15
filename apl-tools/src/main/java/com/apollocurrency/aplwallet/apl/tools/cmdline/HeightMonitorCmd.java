/*
 * Copyright © 2018-2019 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.tools.cmdline;

import com.apollocurrency.aplwallet.apl.tools.IntegerListConverter;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.util.List;

/**
 *
 * @author alukin@gmail.com
 */
@Parameters(commandDescription = "Run blockchain height monitor")
public class HeightMonitorCmd {
    public static final String CMD="heightmon";
    @Parameter(names = {"--intervals", "-i"}, listConverter = IntegerListConverter.class, description = "Set intervals (hours) which should be monitored on max fork size. By default will be used intervals - 1, 2, 4, 6, 8, 12, 24, 48, 96 ")
    public List<Integer> intervals;
    @Parameter(names = {"--peers", "-pr"}, description = "Absolute path to peer ips, which should be monitored. By default will be used file 'peers.txt' in current working directory")
    public String peerFile;
    @Parameter(names = {"--frequency", "-f"}, description = "Fork check frequency in millis. By default is set to 30 seconds")
    public Integer frequency;
    @Parameter(names = {"--port", "-p"}, description = "Set api port for peers to send getBlocks requests. By default is set to 7876")
    public Integer port;

}
