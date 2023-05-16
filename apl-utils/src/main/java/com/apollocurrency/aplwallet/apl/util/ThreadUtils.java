/*
 * Copyright (c)  2018-2019. Apollo Foundation.
 */

package com.apollocurrency.aplwallet.apl.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ThreadUtils {
    private ThreadUtils() {
    }

    public static void sleep(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void sleep(long time, TimeUnit timeUnit) {
        try {
            timeUnit.sleep(time);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static String last3Stacktrace() {
        return lastNStacktrace(3);
    }

    public static String last5Stacktrace() {
        return lastNStacktrace(5);
    }

    public static String lastNStacktrace(int n) {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        List<String> stacktracesToJoin = new ArrayList<>();
        for (int i = Math.min(n + 2, stackTraceElements.length - 1); i >= 3; i--) {
            stacktracesToJoin.add(getStacktraceSpec(stackTraceElements[i]));
        }

        return String.join("->", stacktracesToJoin);
    }

    public static String lastStacktrace() {
        return lastStacktrace(Thread.currentThread().getStackTrace());
    }

    public static String lastStacktrace(StackTraceElement[] stackTraceElements) {
        return lastStacktrace(stackTraceElements, stackTraceElements.length);
    }

    public static String lastStacktrace(StackTraceElement[] stackTraceElements, int elementNumber) {
        StringBuilder stackTrace = new StringBuilder("Trace=");
        int first = Math.min(elementNumber, stackTraceElements.length - 1);
        int last = 0;
        for (int i = first; i >= last; i--) {
            if (i != first) {
                stackTrace.append("->");
            }
            stackTrace.append(getStacktraceSpec(stackTraceElements[i]));
        }
        return stackTrace.toString();
    }

    public static String getStacktraceSpec(StackTraceElement element) {
        String className = element.getClassName();
        return className.substring(className.lastIndexOf(".") + 1) + "." + element.getMethodName() + ":" + element.getLineNumber();
    }

    public static String getStackTrace(Throwable exception) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PrintWriter printWriter = new PrintWriter(out)) {
            exception.printStackTrace(printWriter);
            printWriter.flush();
            byte[] bytes = out.toByteArray();
            return new String(bytes);
        }
    }

    public static String getStackTraceSilently(Throwable exception) {
        try {
            return getStackTrace(exception);
        } catch (IOException e) {
            return e.getMessage() + "(unable to extract stacktrace)";
        }
    }
}
