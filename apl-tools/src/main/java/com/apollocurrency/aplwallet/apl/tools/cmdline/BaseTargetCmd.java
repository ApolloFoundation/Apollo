/*
 * Copyright © 2018 Apollo Foundation
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
@Parameters(commandDescription = "Calulate Apollo base target")
public class BaseTargetCmd {
    public static final String CMD="basetarget"; 
    @Parameter(description = "[start height]")
    public List<String> parameters = new ArrayList<>(); 
}
