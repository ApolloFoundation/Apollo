package com.apollocurrency.aplwallet.apl.util.env.config;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.apollocurrency.aplwallet.apl.util.env.dirprovider.ConfigDirProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ChainsConfigLoader extends AbstractConfigLoader<Map<UUID, Chain>> {
    private static final ObjectMapper MAPPER = JSON.getMapper();
    private static final String DEFAULT_CHAINS_FILENAME = "chains.json";

    public ChainsConfigLoader(boolean ignoreResources, String configDir, String resourceName) {
        super(ignoreResources, configDir, resourceName);
    }

    public ChainsConfigLoader(String resourceName) {
        super(resourceName);
    }

    public ChainsConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources, String resourceName) {
        super(dirProvider, ignoreResources, resourceName);
    }

    public ChainsConfigLoader(boolean ignoreResources, String configDir) {
        super(ignoreResources, configDir, DEFAULT_CHAINS_FILENAME);
    }

    public ChainsConfigLoader() {
        super(DEFAULT_CHAINS_FILENAME);
    }

    public ChainsConfigLoader(ConfigDirProvider dirProvider, boolean ignoreResources) {
        super(dirProvider, ignoreResources, DEFAULT_CHAINS_FILENAME);
    }

    public ChainsConfigLoader(ConfigDirProvider dirProvider, String configDir, boolean ignoreResources) {
        super(dirProvider, ignoreResources, configDir, DEFAULT_CHAINS_FILENAME);
    }


    @Override
    protected Map<UUID, Chain> read(InputStream is) throws IOException {
        List<Chain> chains = MAPPER.readValue(is, new TypeReference<List<Chain>>() {});
        return listToMap(chains);
    }

    @Override
    protected Map<UUID, Chain> merge(Map<UUID, Chain> oldChains, Map<UUID, Chain> newChains) {
        Map<UUID, Chain> resultChains = new HashMap<>();
        if (oldChains != null) {
            resultChains.putAll(oldChains);
        }
        if (newChains != null) {
            resultChains.putAll(newChains);
        }
        System.out.println("Old chains: " + oldChains + " \nNew chains: " + newChains + " \nResult chains: " + resultChains);
        return resultChains;
    }

    private Map<UUID, Chain> listToMap(List<Chain> chains) {
        return chains.stream().collect(Collectors.toMap(Chain::getChainId, Function.identity()));
    }

}
