package com.apollocurrency.aplwallet.apl.tools;

import java.nio.file.Paths;

import ch.qos.logback.core.PropertyDefinerBase;
//TODO: is it right to write logs to current dir?
public class ToolsLogDirDefiner extends PropertyDefinerBase {
    @Override
    public String getPropertyValue() {
        return Paths.get("").resolve("apl-blockchain-tools-logs").toAbsolutePath().toString();
    }
}
