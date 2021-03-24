/*
 * Copyright © 2018-2021 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.conf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 *
 * @author Oleksiy Lukin alukin@gmail.com
 */
public class DefaultConfig {
    @Getter
    final Map<String, ConfigRecord> knownProperties = new HashMap<>();

    public static DefaultConfig fromStream(InputStream is) throws IOException {
        DefaultConfig conf = new DefaultConfig();
        List<String> commentBuf = new ArrayList<>();
        try ( BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                line=line.trim();
                if(line.startsWith("#")){
                    commentBuf.add(line.substring(1));
                }else if (line.isBlank()){
                    commentBuf.clear();
//TODO: define key words for deprecation and introduction version
//TODO: define key words for command line options and environment variables
                }else{
                    String[] parts = line.split("=");
                    String key=parts[0];
                    String value="";
                    if(parts.length > 1){
                     value = parts[1];
                    }
                    int idx = value.indexOf("#");
                    if(idx>=0){
                        value=value.substring(idx);
                    }
                    String comment = commentBuf.stream().collect(Collectors.joining(" "));
                    ConfigRecord cr = ConfigRecord.builder()
                            .description(comment)
                            .name(key)
                            .defaultValue(value)
                            .build();
                    conf.knownProperties.put(key, cr);
                }
            }
        }
        return conf;
    }
}
