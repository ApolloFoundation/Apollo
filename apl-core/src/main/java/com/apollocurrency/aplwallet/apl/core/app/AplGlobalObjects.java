/*
 * Copyright © 2018 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.core.chainid.Chain;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainIdService;
import com.apollocurrency.aplwallet.apl.core.chainid.ChainIdServiceImpl;
import com.apollocurrency.aplwallet.apl.udpater.intfce.UpdaterCore;
import com.apollocurrency.aplwallet.apl.util.env.DirProvider;
import com.apollocurrency.aplwallet.apl.util.NtpTime;
import org.slf4j.Logger;

/**
 * This class required for global accessed objects initialization without static initialization in Apl class
 *
 */
//TODO: temporal class, should be deleted, when DI will be implemented
public class AplGlobalObjects {
    private static final Logger LOG = getLogger(AplGlobalObjects.class);
    private static final Map<String, GlobalObject<?>> OBJECTS = new ConcurrentHashMap<>();
    private static final String DEFAULT_INIT_ERROR = "%s was not initialized before accessing";
    private static final String DEFAULT_CHAINID_SERVICE_NAME = "ChainIdService";
    private static final String DEFAULT_CHAIN_CONFIG_NAME = "BlockchainConfig";
    private static final String DEFAULT_PROPERTIES_LOADER_NAME = "PropertiesLoader";
    private static final String DEFAULT_UPDATER_CORE_NAME = "UpdaterCore";
    private static final String DEFAULT_NTP_TIME_NAME = "NtpTime";
    private static final String GET_EXEPTION_TEMPLATE = "Unable to get %s. %s is not an instance of %s";


    public static void createChainIdService(String chainIdFilePath) {
        ChainIdService chainIdService = new ChainIdServiceImpl(chainIdFilePath == null ? "chains.json" : chainIdFilePath);
        save(DEFAULT_CHAINID_SERVICE_NAME, new GlobalObject<>(chainIdService, DEFAULT_CHAINID_SERVICE_NAME));
    }


    private static<T> void save(String name, GlobalObject<T> object) {
        OBJECTS.put(name, object);
        LOG.info("Saved new {} object as instance of {}", name, object.getObj().getClass());
    }

    public static<T> void set(GlobalObject<T> globalObject) {
        if (globalObject == null || !globalObject.isValid()) {
            throw new IllegalArgumentException("Global object is not valid!");
        }
        save(globalObject.getName(), globalObject);
    }


    public static void createUpdaterCore(boolean doInit) {
//        UpdaterCore updaterCore = new UpdaterCoreImpl(new UpdaterMediatorImpl());
//        if (doInit) {
//            updaterCore.init();
//        }
//        save(DEFAULT_UPDATER_CORE_NAME, new GlobalObject<>(updaterCore, DEFAULT_UPDATER_CORE_NAME));
    }

    public static void createNtpTime() {
        NtpTime ntpTime = new NtpTime();
        save(DEFAULT_NTP_TIME_NAME, new GlobalObject<>(ntpTime, DEFAULT_NTP_TIME_NAME));
    }
    public static void createPropertiesLoader(DirProvider dirProvider, boolean doInit) {
        PropertiesLoader propertiesLoader = new PropertiesLoader.Builder(dirProvider).build();
        if (doInit) {
            propertiesLoader.init();
        }
        save(DEFAULT_PROPERTIES_LOADER_NAME, new GlobalObject<>(propertiesLoader, DEFAULT_PROPERTIES_LOADER_NAME));
    }
    public static void createPropertiesLoader(DirProvider dirProvider) {
        createPropertiesLoader(dirProvider, true);
    }

    public static void createBlockchainConfig(Chain chain, PropertiesLoader loader, boolean doInit) {
        BlockchainConfig blockchainConfig = new BlockchainConfig(chain, loader);
        if (doInit) {
            blockchainConfig.init();
        }
        OBJECTS.put(DEFAULT_CHAIN_CONFIG_NAME, new GlobalObject<>(blockchainConfig, DEFAULT_CHAIN_CONFIG_NAME));
    }
    public static void createBlockchainConfig(Chain chain, PropertiesLoader loader) {
        createBlockchainConfig(chain, loader, true);
    }

    private static <T> T get(Class<T> clazz, String name) {
        GlobalObject o = OBJECTS.get(name);
        validateInitialization(o, name);
        Object realObject = o.getObj();
        if (clazz.isInstance(realObject)) {
            return clazz.cast(realObject);
        } else {
            throw new RuntimeException(String.format(GET_EXEPTION_TEMPLATE, name, realObject.getClass(), clazz));
        }
    }
    public static PropertiesLoader getPropertiesLoader() {
        return get(PropertiesLoader.class, DEFAULT_PROPERTIES_LOADER_NAME);
    }

    private static void validateInitialization(Object object, String component) {
        if (object == null) {
            throw new RuntimeException(String.format(DEFAULT_INIT_ERROR, component));
        }
    }

    public static BlockchainConfig getChainConfig() {
        return get(BlockchainConfig.class, DEFAULT_CHAIN_CONFIG_NAME);
    }

    public static ChainIdService getChainIdService() {
        return get(ChainIdService.class, DEFAULT_CHAINID_SERVICE_NAME);
    }
    public static UpdaterCore getUpdaterCore() {
        return get(UpdaterCore.class, DEFAULT_UPDATER_CORE_NAME);
    }
}
