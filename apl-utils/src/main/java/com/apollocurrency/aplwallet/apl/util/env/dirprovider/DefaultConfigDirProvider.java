/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.util.env.dirprovider;

import java.nio.file.Paths;

/**
 * Config dir provider which provide default config files locations
 */
public class DefaultConfigDirProvider implements ConfigDirProvider {
    protected String applicationName;
    protected boolean isService;
    protected int netIndex;
    protected static final String[] CONF_DIRS={"conf","conf-tn1","conf-tn2","conf-tn3"};
/**
 * Constructs config dir provider
 * @param applicationName name of application's parameter dir
 * @param isService service mode or user mode
 * @param netIdx index of network. 0 means main net, 1,2,3 - testnets 1,2,3
 */
    public DefaultConfigDirProvider(String applicationName, boolean isService, int netIdx) {
        if (applicationName == null || applicationName.trim().isEmpty()) {
            throw new IllegalArgumentException("Application name cannot be null or empty");
        }
        this.applicationName = applicationName.trim();
        this.isService = isService;
        if(netIdx<=0){
            this.netIndex = 0;
        }else if (netIdx>CONF_DIRS.length-1){
            this.netIndex=CONF_DIRS.length-1;
        }else{
            this.netIndex=netIdx;
        }
    }

    @Override
    public String getConfigDirectoryName(){
        return CONF_DIRS[netIndex];
    }
    @Override
    public String getInstallationConfigDirectory() {
        return DirProvider.getBinDir().resolve(getConfigDirectoryName()).toAbsolutePath().toString();
    }

    @Override
    public String getSysConfigDirectory() {
        return getInstallationConfigDirectory();
    }

    @Override
    public String getUserConfigDirectory() {
        String res = Paths.get(System.getProperty("user.home"), "." + applicationName, getConfigDirectoryName()).toAbsolutePath().toString();
        return res;
    }

    @Override
    public String getConfigDirectory() {
        String res =
            isService
                ? getSysConfigDirectory()
                : getUserConfigDirectory();
        return res;

    }
}
