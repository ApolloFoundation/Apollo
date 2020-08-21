package com.apollocurrency.apl.id.utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author al
 */
public class StringList {
    public static String fromList(List<String> sl) {
        String res = "";
        for (int i = 0; i < sl.size(); i++) {
            String semicolon = i < sl.size() - 1 ? ";" : "";
            res += sl.get(i) + semicolon;
        }
        return res;
    }

    public static List<String> fromString(String l) {
        List<String> res = new ArrayList<>();
        String[] ll = l.split(";");
        for (String s : ll) {
            if (!s.isEmpty()) {
                res.add(s);
            }
        }
        return res;
    }    
}
