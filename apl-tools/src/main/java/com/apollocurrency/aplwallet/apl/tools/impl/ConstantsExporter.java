/*
 * Copyright © 2013-2016 The Nxt Core Developers.
 * Copyright © 2016-2017 Jelurida IP B.V.
 *
 * See the LICENSE.txt file at the top-level directory of this distribution
 * for licensing information.
 *
 * Unless otherwise agreed in a custom licensing agreement with Jelurida B.V.,
 * no part of the Nxt software, including this file, may be copied, modified,
 * propagated, or distributed except according to the terms contained in the
 * LICENSE.txt file.
 *
 * Removal or modification of this copyright notice is prohibited.
 *
 */

/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.tools.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.apollocurrency.aplwallet.apl.core.http.get.GetConstants;
import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.env.PosixExitCodes;

public class ConstantsExporter {
    public static int export(String path) {
        int res = PosixExitCodes.OK.exitCode();
        //TODO: stdout output for piped execution
        try {
            Path filePath = Paths.get(path);
            Files.write(filePath, (String.format("if (!ARS) {%1$s" +
                    "    var ARS = {};%1$s" +
                    "    ARS.constants = {};%1$s" +
                    "}%1$s%1$s",System.lineSeparator())).getBytes());
            Files.write(filePath, ("ARS.constants.SERVER = ").getBytes(), StandardOpenOption.APPEND);
            JSON.writeJSONString(GetConstants.getConstants(), filePath);
            Files.write(filePath, String.format("%1$s%1$s" +
                    "if (isNode) {%1$s" +
                    "    module.exports = ARS.constants.SERVER;%1$s" +
                    "}%1$s", System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {            
            e.printStackTrace();
            res = PosixExitCodes.EX_IOERR.exitCode();
        }
        return res;
    }
}
