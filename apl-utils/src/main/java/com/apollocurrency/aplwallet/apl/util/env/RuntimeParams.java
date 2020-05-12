/*
 * Copyright © 2018 Apollo Foundation
 */
package com.apollocurrency.aplwallet.apl.util.env;

import java.net.ServerSocket;

/**
 * Runtime-related parameters and routines
 *
 * @author alukin@gmail.com
 */
public class RuntimeParams {

    public static String getProcessId() {
        Long pid = ProcessHandle.current().pid();
        return pid.toString();
    }

    public static boolean isTcpPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean isAMD64Architecture() {
        String arch = System.getProperty("os.arch");
        return arch.equalsIgnoreCase("amd64") || arch.equalsIgnoreCase("x86_64");
    }

    public String getUserName() {
        return System.getProperty("user.name");
    }
}
