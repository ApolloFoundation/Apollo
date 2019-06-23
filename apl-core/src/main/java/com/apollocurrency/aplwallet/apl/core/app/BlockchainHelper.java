/*
 * Copyright © 2018-2019 Apollo Foundation
 */

package com.apollocurrency.aplwallet.apl.core.app;

import com.apollocurrency.aplwallet.apl.core.app.Blockchain;
import com.apollocurrency.aplwallet.apl.core.chainid.BlockchainConfig;
import com.apollocurrency.aplwallet.apl.util.injectable.PropertiesHolder;

import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BlockchainHelper {

    private static final PropertiesHolder propertiesHolder = CDI.current().select(PropertiesHolder.class).get();
    private static final Blockchain blockchain = CDI.current().select(Blockchain.class).get();

    public static int getBlockchainHeight(){
        return blockchain.getHeight();
    }

    public static boolean checkBlock(long blockId){
        return blockchain.hasBlock(blockId);
    }

    public static boolean checkLastBlock(long blockId){
        return blockId != blockchain.getLastBlock().getId();
    }

}
