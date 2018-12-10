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
    
    public String getUserName(){
        return System.getProperty("user.name");
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
}
