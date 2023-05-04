package com.apollocurrency.aplwallet.apl.exec;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class CmdLineArgsTest {

    String[] allArgStringArray = {
        "--debug", "4",
        "--debug-updater", "true",
        "--help", "true",
        "--service-mode", "true",
        "--ignore-resources", "true",
        "--config-dir", "/tmp",
        "--log-dir", "/tmp",
        "--db-dir", "/tmp",
        "--vault-key-dir", "/tmp",
        "--dex-key-dir", "/tmp",
        "--no-shard-import",
        "--no-shard-create",
        "--update-attachment-file", "attach-file.bin",
        "--2fa-dir", "/tmp",
        "--dexp-dir", "/tmp",
        "--pid-file", "/tmp/1.pid",
        "--net", "3",
        "--chain", "chains-1.json",
        "--testnet", "1",
        "--disable-weld-concurrent-deployment", "false"
    };

    String[] allArgShortArray = {
        "-d", "4",
        "-du", "true",
        "-h", "true",
        "-s", "true",
        "--ignore-resources", "true",
        "-c", "/tmp",
        "-l", "/tmp",
        "--db-dir", "/tmp",
        "--vault-key-dir", "/tmp",
        "--dex-key-dir", "/tmp",
        "--no-shard-import",
        "--no-shard-create",
        "-u", "attach-file.bin",
        "--2fa-dir", "/tmp",
        "--dexp-dir", "/tmp",
        "--pid-file", "/tmp/1.pid",
        "-n", "3",
        "-C", "chains-1.json",
        "--testnet", "1",
        "--disable-weld-concurrent-deployment", "false"
    };

    @Test
    void testAllArgs_longStringValues() {
        CmdLineArgs lineArgs = new CmdLineArgs();
        CommandLine.ParseResult result = new CommandLine(lineArgs).parseArgs(allArgStringArray);
        log.debug("args = {}, result = {}", allArgStringArray, result);
        assertEquals(0, result.errors().size());
    }

    @Test
    void testAllArgs_emptyValues() {
        CmdLineArgs lineArgs = new CmdLineArgs();
        String[] emptyArgs = {};
        CommandLine.ParseResult result = new CommandLine(lineArgs).parseArgs(emptyArgs);
        log.debug("args = {}, result = {}", allArgStringArray, result);
        assertEquals(0, result.errors().size());
    }

    @Test
    void testAllArgs_shortStringValues() {
        CmdLineArgs lineArgs = new CmdLineArgs();
        CommandLine.ParseResult result = new CommandLine(lineArgs).parseArgs(allArgShortArray);
        log.debug("args = {}, result = {}", allArgStringArray, result);
        assertEquals(0, result.errors().size());
    }

    @Test
    void testAllArgs_unknownStringValues() {
        CmdLineArgs lineArgs = new CmdLineArgs();
        String[] unknownArgsArray = {"--unknown-param", "-UP"};
        Exception error = assertThrows(CommandLine.UnmatchedArgumentException.class,
            () -> new CommandLine(lineArgs).parseArgs(unknownArgsArray)
        );
        log.debug("args = {}, error = {}", Arrays.toString(unknownArgsArray), error.getMessage());
        assertEquals("Unknown options: " +
            Arrays.stream(unknownArgsArray).map(String::valueOf).reduce((a, b) -> "'" + a + "', '" + b).get() + "'",
            error.getMessage());
    }

}