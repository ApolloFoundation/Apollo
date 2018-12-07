package com.apollocurrency.aplwallet.apl.util.env;

import java.lang.management.ManagementFactory;
import java.net.ServerSocket;

/**
 * Runtime-related parameters and routines
 *
 * @author alukin@gmail.com
 */
public class RuntimeParams {

    public static String getProcessId() {
        String runtimeName = ManagementFactory.getRuntimeMXBean().getName();
        if (runtimeName == null) {
            return "";
        }
        String[] tokens = runtimeName.split("@");
        if (tokens.length == 2) {
            return tokens[0];
        }
        return "";
    }

    public static boolean isTcpPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
