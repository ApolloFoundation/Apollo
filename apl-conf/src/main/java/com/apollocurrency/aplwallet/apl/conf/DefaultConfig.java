/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.conf;

import com.apollocurrency.aplwallet.apl.util.Version;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Parser of default config from resources.
 * Default config must contain all properties known to the program with default values,
 * description and keywords signaling corresponding command line options and
 * environment variables. Keywords are prefixed
 * @author Oleksiy Lukin alukin@gmail.com
 */
@Slf4j
public class DefaultConfig {

    public static final String KW_DEPRECATED = "DEPRECATED";
    public static final String KW_SINCE = "SINCE";
    public static final String KW_COMMAND_LINE = "CMDLINE";
    public static final String KW_ENV_VAR = "ENVVAR";

    @Getter
    final Map<String, ConfigRecord> knownProperties = new HashMap<>();

    static ConfigRecord createCr(String key, String value, List<String> comments) {
        Objects.requireNonNull(key, "key is NULL");
        Objects.requireNonNull(value, "value is NULL");
        Objects.requireNonNull(comments, "comments list is NULL");
        String comment = comments.stream().collect(Collectors.joining(" "));
        ConfigRecord cr = ConfigRecord.builder()
                .name(key)
                .defaultValue(value)
                .description(comment)
                .build();
        for (String line : comments) {
            if (line.startsWith("!")) {
                int end = line.indexOf(" ")>3 ? line.indexOf(" "): line.length()-1;
                String cmd = line.substring(1,end).trim();
                String val = line.substring(end).trim();
                int val_end = val.indexOf(" ")>0 ? val.indexOf(" "): val.length();
                val = val.substring(0,val_end);
                switch(cmd) {
                    case KW_DEPRECATED:
                        cr.deprecatedSince = new Version(val);
                        break;
                    case KW_SINCE:
                        cr.sinceRelease = new Version(val);
                        break;
                    case KW_COMMAND_LINE:
                        cr.cmdLineOpt=val;
                        break;
                    case KW_ENV_VAR:
                        cr.envVar = val;
                        break;
                    default:
                        log.warn("Unknown keyword in the property comments. Property: {} Keyword: {}", key, cmd);
                }
            }
        }

        return cr;
    }

    public static DefaultConfig fromStream(InputStream is) throws IOException {
        Objects.requireNonNull(is, "Input Stream is NULL");
        DefaultConfig conf = new DefaultConfig();
        List<String> commentBuf = new ArrayList<>();
        try ( BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("#")) {
                    commentBuf.add(line.substring(1));
                } else if (line.isBlank()) {
                    commentBuf.clear();
                } else {
                    String[] parts = line.split("=");
                    String key = parts[0];
                    String value = "";
                    if (parts.length > 1) {
                        value = parts[1];
                    }
                    int idx = value.indexOf("#");
                    if (idx >= 0) {
                        value = value.substring(idx);
                    }

                    ConfigRecord cr = createCr(key, value, commentBuf);
                    conf.knownProperties.put(key, cr);
                }
            }
        }
        return conf;
    }
}
