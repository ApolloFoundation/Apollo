package com.apollocurrency.aplwallet.apl.exec;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        "--no-shard-import", "true",
        "--no-shard-create", "true",
        "--update-attachment-file", "attach-file.bin",
        "--2fa-dir", "/tmp",
        "--dexp-dir", "/tmp",
        "--pid-file", "/tmp/1.pid",
        "--net", "3",
        "--chain", "chains-1.json",
        "--testnet", "false",
        "--disable-weld-concurrent-deployment", "false"
    };

    String[] allArgShortArray = {
        "-d", "1",
        "-du", "true",
        "-h", "true",
        "-s", "true",
        "--ignore-resources", "true",
        "-c", "/tmp",
        "-l", "/tmp",
        "--db-dir", "/tmp",
        "--vault-key-dir", "/tmp",
        "--dex-key-dir", "/tmp",
        "--no-shard-import", "false",
        "--no-shard-create", "false",
        "-u", "attach-file.bin",
        "--2fa-dir", "/tmp",
        "--dexp-dir", "/tmp",
        "--pid-file", "/tmp/1.pid",
        "-n", "3",
        "-C", "chains-1.json",
        "--testnet", "true",
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
        String[] emptyArgs = {};
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(emptyArgs);
        log.debug("args = {}, result = {}", allArgStringArray, result);
        assertEquals(0, result.errors().size());
    }

    @Test
    void testAllArgs_shortStringValues() {
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(allArgShortArray);
        log.debug("args = {}, result = {}", allArgStringArray, result);
        assertEquals(0, result.errors().size());
    }

    @Test
    void testAllArgs_unknownStringValues() {
        String[] unknownArgsArray = {"--unknown-param", "-UP"};
        Exception error = assertThrows(CommandLine.UnmatchedArgumentException.class,
            () -> new CommandLine(new CmdLineArgs()).parseArgs(unknownArgsArray)
        );
        log.debug("args = {}, error = {}", Arrays.toString(unknownArgsArray), error.getMessage());
        assertEquals("Unknown options: " +
            Arrays.stream(unknownArgsArray).map(String::valueOf).reduce((a, b) -> "'" + a + "', '" + b).get() + "'",
            error.getMessage());
    }

    @ParameterizedTest
    @CsvSource(value = {"--debug-updater", "--help", "--service-mode", "--ignore-resources", "--no-shard-import",
        "--no-shard-create", "--testnet", "--disable-weld-concurrent-deployment",
        "-du", "-h", "-s"
    })
    void test_emptyBooleansEvaluatedToTrue(String paramName) {
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(paramName);
        log.debug("Parameter = {}, result = {}", paramName, result.errors());
        assertEquals(0, result.errors().size());
        assertTrue((Boolean) result.matchedArgs().get(0).typedValues().get(0));
    }

    @ParameterizedTest
    @CsvSource(value = {"--debug-updater=false,false", "--help=false,false", "--service-mode=false,false", "--ignore-resources=false,false",
        "--no-shard-import=false,false", "--no-shard-create=false,false", "--testnet=false,false", "--disable-weld-concurrent-deployment=false,false",
        "-du,true", "-h,true", "-s,true"
    })
    void test_specifiedFalseBooleans(String paramName, String value) {
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(paramName);
        log.debug("Parameter = {}, result = {}", paramName, result.errors());
        assertEquals(0, result.errors().size());
        assertEquals(Boolean.valueOf(value), result.matchedArgs().get(0).typedValues().get(0));
    }

    @ParameterizedTest
    @CsvSource(value = {"--debug-updater=true,true", "--help,true", "--service-mode=true,true", "--ignore-resources=true,true",
        "--no-shard-import=true,true", "--no-shard-create=true,true", "--testnet=true,true", "--disable-weld-concurrent-deployment=true,true",
        "-du,true", "-h,true", "-s,true"
    })
    void test_specifiedTrueBooleans(String paramName, String value) {
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(paramName);
        log.debug("Parameter = {}, result = {}", paramName, result.errors());
        assertEquals(0, result.errors().size());
        assertEquals(Boolean.valueOf(value), result.matchedArgs().get(0).typedValues().get(0));
    }

    @ParameterizedTest
    @CsvSource(value = {"--debug-updater,true", "--help,true", "--service-mode,true", "--ignore-resources,true",
        "--no-shard-import,true", "--no-shard-create,true", "--testnet,true", "--disable-weld-concurrent-deployment,true",
        "-du,true", "-h,true", "-s,true"
    })
    void test_specifiedDefaults(String paramName, String value) {
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(paramName);
        log.debug("Parameter = {}, result = {}", paramName, result.errors());
        assertEquals(0, result.errors().size());
        assertEquals(Boolean.valueOf(value), result.matchedArgs().get(0).typedValues().get(0));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "--config-dir=/tmp,/tmp",
        "--log-dir=/tmp,/tmp",
        "--db-dir=/tmp,/tmp",
        "--vault-key-dir=/tmp,/tmp",
        "--dex-key-dir=/tmp,/tmp",
        "--update-attachment-file=attach-file.bin,attach-file.bin",
        "--2fa-dir=/tmp,/tmp",
        "--dexp-dir=/tmp,/tmp",
        "--pid-file=/tmp/1.pid,/tmp/1.pid",
        "--chain=chains-1.json,chains-1.json",
    })
    void test_longStringParamsSpecified(String paramName, String value) {
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(paramName);
        log.debug("Parameter = {}, result = {}", paramName, result.errors());
        assertEquals(0, result.errors().size());
        assertEquals(value, result.matchedArgs().get(0).typedValues().get(0));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "-c /tmp,/tmp",
        "-l /tmp,/tmp",
        "-u attach-file.bin,attach-file.bin",
        "-C chains-1.json,chains-1.json"
    })
    void test_shortStringParamsSpecified(String paramName, String value) {
        CommandLine.ParseResult result = new CommandLine(new CmdLineArgs()).parseArgs(paramName);
        log.debug("Parameter = {}, result = {}", paramName, result.errors());
        assertEquals(0, result.errors().size());
        assertEquals(value, result.matchedArgs().get(0).typedValues().get(0).toString().trim());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "--config-dir",
        "--log-dir",
        "--db-dir",
        "--vault-key-dir",
        "--dex-key-dir",
        "--update-attachment-file",
        "--2fa-dir",
        "--dexp-dir",
        "--pid-file",
        "--chain",
    })
    void test_errorWhenStringParamsIsNotSpecified(String paramName) {
        Exception error = assertThrows(CommandLine.MissingParameterException.class,
            () -> new CommandLine(new CmdLineArgs()).parseArgs(paramName)
        );
        log.debug("Parameter = {}, error = {}", paramName, error.getMessage());
        assertTrue(error.getMessage().startsWith("Missing required parameter for option '" + paramName + "'"));
    }

}