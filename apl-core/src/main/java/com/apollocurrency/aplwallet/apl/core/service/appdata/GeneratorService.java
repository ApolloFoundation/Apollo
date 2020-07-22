package com.apollocurrency.aplwallet.apl.core.service.appdata;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.apollocurrency.aplwallet.apl.core.entity.appdata.GeneratorEntity;
import com.apollocurrency.aplwallet.apl.core.entity.blockchain.Block;
import com.apollocurrency.aplwallet.apl.core.service.blockchain.BlockchainProcessor;

public interface GeneratorService {

    GeneratorEntity startForging(byte[] keySeed);

    GeneratorEntity stopForging(byte[] keySeed);

    int stopForging();

    GeneratorEntity getGenerator(long id);

    int getGeneratorCount();

    Map<Long, GeneratorEntity> getGeneratorsMap();

    Collection<GeneratorEntity> getAllGenerators();

    List<GeneratorEntity> getSortedForgers();

    long getNextHitTime(long lastBlockId, int curTime);

    void setDelay(int delay);

    boolean verifyHit(GeneratorEntity generator, Block previousBlock, int timestamp);

    void setLastBlock(Block lastBlock, GeneratorEntity generator);

    BigInteger getHit(byte[] publicKey, Block block);

    long getHitTime(BigInteger effectiveBalance, BigInteger hit, Block block);

    void suspendForging();

    void resumeForging();

    boolean forge(Block lastBlock, int generationLimit, GeneratorEntity generator) throws BlockchainProcessor.BlockNotAcceptedException;

}
