/*
 * Copyright © 2020 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.supervisor.client.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Path specification as in JAX-RS example: /messages/{id}/{code} All values are
 * handled as strings, it is up to processing code how to convert it
 *
 * @author alukin@gmail.com
 */
@Slf4j
public class PathSpecification {

    public String prefix;
    public List<String> paramNames = new ArrayList<>();
    public String pathSpec;

    public PathSpecification() {
    }

    public static PathSpecification fromSpecString(String pathSpec) {
        PathSpecification res = new PathSpecification();
        res.pathSpec = pathSpec;
        String[] elements = pathSpec.split("/");
        int bracePos = pathSpec.indexOf("{");
        if (bracePos > 0) {
            res.prefix = pathSpec.substring(0, bracePos - 1);
            for (String element : elements) {
                if (element.startsWith("{")) {
                    if (element.endsWith("}")) {
                        res.paramNames.add(element.substring(1, element.length() - 1));
                    } else { //TODO consider throwing exception here
                        log.error("Missing brace in path element {} of path sepce: {}", element, pathSpec);
                        res.paramNames.add(element.substring(1, element.length()));
                    }
                }
            }
        } else {
            res.prefix = pathSpec;
        }

        return res;
    }

    public boolean matches(String path) {
        boolean res = false;
        int len = prefix.length();
        if (path.length() >= len) {
            String matching = path.substring(0, len);
            if (matching.equals(prefix)) {
                res = true;
            }
        }
        return res;
    }

    public Map<String, String> parseParams(String path) {
        Map<String, String> res = new HashMap<>();
        String params = path.substring(prefix.length());
        if (!params.isEmpty()) {
            String[] elements = params.split("/");
            //first element after split() may be ""
            int start = 0;
            if (elements.length > 0) {
                if (elements[0].isEmpty()) {
                    start = 1;
                }
            }
            for (int i = start; i < elements.length; i++) {
                if (i >= paramNames.size() + start) {
                    break;
                }
                res.put(paramNames.get(i - start), elements[i]);
            }
        }
        return res;
    }
}
