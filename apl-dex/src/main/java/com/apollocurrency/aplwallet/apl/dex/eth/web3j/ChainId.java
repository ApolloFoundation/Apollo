/*
 *  Copyright © 2018-2021 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.dex.eth.web3j;

import com.apollocurrency.aplwallet.apl.dex.eth.service.DexBeanProducer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.NetVersion;
import org.web3j.protocol.exceptions.ClientConnectionException;
import org.web3j.tx.ChainIdLong;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
@Slf4j
public class ChainId {
    private final DexBeanProducer dexBeanProducer;

    private volatile byte chainId = (byte) ChainIdLong.NONE;
    @Getter
    private volatile boolean initialized;
    @Getter
    private volatile boolean failed;

    @Inject
    public ChainId(DexBeanProducer dexBeanProducer) {
        this.dexBeanProducer = dexBeanProducer;
    }

    @PostConstruct
    public void init() {
        initChainId();
        initialized = true;
    }

    public byte get() {
        if (!initialized) {
            throw new IllegalStateException("Chain id is not initialized");
        }
        return chainId;
    }

    public byte getValid() {
        validate();
        return chainId;
    }

    public void validate() {
        if (!initialized) {
            throw new IllegalStateException("Chain id is not initialized");
        }
        if (failed) {
            throw new IllegalStateException("Chain id failed to init");
        }
    }

    private void initChainId() {
        Request<?, NetVersion> netVersionRequest = dexBeanProducer.web3j().netVersion();
        NetVersion netVersionResponse;
        try {
            netVersionResponse = netVersionRequest.send();
        } catch (IOException | ClientConnectionException e) {
            log.error("Unable to get chain id for the Ethereum network", e);
            failed = true;
            return;
        }
        if (netVersionResponse.hasError()) {
            log.error("Error getting chain id for the Ethereum network, code: {}, message: {}", netVersionResponse.getError().getCode(), netVersionResponse.getError().getMessage());
            failed = true;
            return;
        }
        String netVersionString = netVersionResponse.getNetVersion();
        byte netVersion = (byte) Long.parseUnsignedLong(netVersionString);
        log.info("Connected to the {} Ethereum Chain ", ChainIdLong.MAINNET == netVersion ? "MAINNET" : ChainIdLong.ROPSTEN == netVersion ? "ROPSTEN" : netVersionString);
        chainId = netVersion;
    }

}
