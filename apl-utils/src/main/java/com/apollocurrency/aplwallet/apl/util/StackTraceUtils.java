package com.apollocurrency.aplwallet.apl.util;

import javax.enterprise.inject.Vetoed;
import java.util.Arrays;
import java.util.stream.Collectors;
@Vetoed
public class StackTraceUtils {
    public static String lastNStacktrace(int n) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        return String.join("->", Arrays.stream(stackTraceElements).skip(3).limit(n).map(StackTraceUtils::getStacktraceSpec).collect(Collectors.joining("<-")));
    }

    private static String getStacktraceSpec(StackTraceElement element) {
        String className = element.getClassName();
        return className.substring(className.lastIndexOf(".") + 1) + "." + element.getMethodName();
    }
    private StackTraceUtils(){}
}
