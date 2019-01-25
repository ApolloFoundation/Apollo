/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.chainid;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import com.apollocurrency.aplwallet.apl.util.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Extract  chains from config file and provide current active chain
 * @see Chain
 * @see BlockchainConfig
 */
@Singleton
public class ChainIdServiceImpl implements ChainIdService {
    private static final String DEFAULT_CONFIG_LOCATION = "conf/chains.json";
    private final String chainsConfigFileLocations;
    private static final ObjectMapper MAPPER = JSON.getMapper();

    public ChainIdServiceImpl(String chainsConfigFileLocation) {
        this.chainsConfigFileLocations = chainsConfigFileLocation;
    }

    public ChainIdServiceImpl() {
        this(DEFAULT_CONFIG_LOCATION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Chain> getAll() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(chainsConfigFileLocations);
        if (is == null) {
            is = Files.newInputStream(Paths.get(chainsConfigFileLocations));
        }
        return MAPPER.readValue(is, new TypeReference<List<Chain>>() {});
    }

    /**
     * {@inheritDoc}
     * @throws RuntimeException when no active chain available
     * @throws RuntimeException when multiple active chains available
     */
    @Override
    public Chain getActiveChain() throws IOException {
        List<Chain> chains = getAll();
        List<Chain> activeChains = chains.stream().filter(Chain::isActive).collect(Collectors.toList());
        if (activeChains.size() == 0) {
            throw new RuntimeException("No active chain specified!");
        } else if (activeChains.size() > 1) {
            throw new RuntimeException("Only one chain can be active at the moment!");
        }
        return activeChains.get(0);
    }
}
